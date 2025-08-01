/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.sun.jdi.*
import com.sun.jdi.event.*
import com.sun.jdi.request.EventRequest.SUSPEND_ALL
import com.sun.jdi.request.StepRequest
import com.sun.tools.jdi.SocketAttachingConnector
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.BOX_MAIN_FILE_NAME
import org.jetbrains.kotlin.test.utils.*
import java.io.File
import java.net.URL

private val EXCLUDED_PACKAGES = listOf("java.*", "sun.*", "kotlin.*", "jdk.internal.*", "com.azul.*")

abstract class DebugRunner(testServices: TestServices) : JvmBoxRunner(testServices) {
    companion object {
        val BOX_MAIN_FILE_CLASS_NAME = BOX_MAIN_FILE_NAME.replace(".kt", "Kt")
    }

    private lateinit var wholeFile: File
    private val backend: TargetBackend = testServices.defaultsProvider.targetBackend!!
    private val frontend: FrontendKind<*> = testServices.defaultsProvider.frontendKind

    abstract fun storeStep(loggedItems: ArrayList<SteppingTestLoggedData>, event: Event)

    override fun launchSeparateJvmProcess(
        javaExe: File,
        module: TestModule,
        classPath: List<URL>,
        mainClassAndArguments: List<String>
    ): Process {
        wholeFile = module.files.singleOrNull { it.name == "test.kt" }?.originalFile
            ?: error("There must be a file named 'test.kt' in the test")

        // Setup the java process to suspend waiting for debugging connection on a free port.
        val command = listOfNotNull(
            javaExe.absolutePath,
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:0",
            "-ea",
            "-classpath",
            classPath.joinToString(File.pathSeparator, transform = { File(it.toURI()).absolutePath }),
        ) + mainClassAndArguments

        val process = ProcessBuilder(command).start()

        // Extract the chosen port from the output of the newly started java process.
        // The java process prints a line with the format:
        //
        //      Listening for transport dt_socket at address: <port number>
        val port = process.inputStream.bufferedReader().readLine().split("address:").last().trim().toInt()

        // Attach debugger to the separate java process, setup initial event requests,
        // and run the debugger loop to step through the program.
        val virtualMachine = attachDebugger(port)
        setupMethodEntryAndExitRequests(virtualMachine)
        runDebugEventLoop(virtualMachine)

        return process
    }

    // Debug event loop to step through a test program.
    private fun runDebugEventLoop(virtualMachine: VirtualMachine) {
        val manager = virtualMachine.eventRequestManager()
        val loggedItems = ArrayList<SteppingTestLoggedData>()
        var inBoxMethod = false
        vmLoop@
        while (true) {
            val eventSet = virtualMachine.eventQueue().remove(1000) ?: continue
            for (event in eventSet) {
                when (event) {
                    is VMDeathEvent, is VMDisconnectEvent -> {
                        break@vmLoop
                    }
                    // We start VM with option 'suspend=n', in case VMStartEvent is still received, discard.
                    is VMStartEvent -> {

                    }
                    is MethodEntryEvent -> {
                        if (!inBoxMethod && event.location().method().name() == "box") {
                            if (manager.stepRequests().isEmpty()) {
                                // Create line stepping request to get all normal line steps starting now.
                                val stepReq = manager.createStepRequest(event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO)
                                stepReq.setSuspendPolicy(SUSPEND_ALL)
                                EXCLUDED_PACKAGES.forEach { stepReq.addClassExclusionFilter(it) }
                                // Create class prepare request to be able to set breakpoints on class initializer lines.
                                // There are no line stepping events for class initializers, so we depend on breakpoints.
                                val prepareReq = manager.createClassPrepareRequest()
                                prepareReq.setSuspendPolicy(SUSPEND_ALL)
                                EXCLUDED_PACKAGES.forEach { prepareReq.addClassExclusionFilter(it) }
                            }
                            manager.stepRequests().map { it.enable() }
                            manager.classPrepareRequests().map { it.enable() }
                            inBoxMethod = true
                            storeStep(loggedItems, event)
                        }
                    }
                    is StepEvent -> {
                        if (inBoxMethod) {
                            // Handle the case where an Exception causing program to exit without MethodExitEvent.
                            if (event.location().method().name() == "main" &&
                                event.location().declaringType().name().contains(BOX_MAIN_FILE_CLASS_NAME)
                            ) {
                                manager.stepRequests().map { it.disable() }
                                manager.classPrepareRequests().map { it.disable() }
                                manager.breakpointRequests().map { it.disable() }
                                break@vmLoop
                            }
                            storeStep(loggedItems, event)
                        }
                    }
                    is MethodExitEvent -> {
                        if (event.location().method().name() == "box") {
                            manager.stepRequests().map { it.disable() }
                            manager.classPrepareRequests().map { it.disable() }
                            manager.breakpointRequests().map { it.disable() }
                            break@vmLoop
                        }
                    }
                    is ClassPrepareEvent -> {
                        if (inBoxMethod) {
                            val initializer = event.referenceType().methods().find { it.isStaticInitializer }
                            try {
                                initializer?.allLineLocations()?.forEach {
                                    manager.createBreakpointRequest(it).enable()
                                }
                            } catch (e: AbsentInformationException) {
                                // If there is no line information, do not set breakpoints.
                            }
                        }
                    }
                    is BreakpointEvent -> {
                        if (inBoxMethod) {
                            storeStep(loggedItems, event)
                        }
                    }
                    else -> {
                        throw IllegalStateException("event not handled: $event")
                    }
                }
            }
            eventSet.resume()
        }
        checkSteppingTestResult(frontend, backend, wholeFile, loggedItems, testServices.defaultDirectives)
        virtualMachine.resume()
    }

    protected fun Location.formatAsExpectation(visibleVars: List<LocalVariableRecord>?): String {
        val fileNames =
            testServices.moduleStructure.modules.flatMap { it.files }.map { it.name } +
                    SourceMapper.FAKE_FILE_NAME
        return formatAsSteppingTestExpectation(
            sourceName(),
            // Do not render line numbers outside of test sources (i.e. from stdlib): they can change and it's not a part of this test.
            lineNumber().takeIf { sourceName() in fileNames },
            method().name(),
            method().isSynthetic,
            visibleVars
        )
    }

    private fun setupMethodEntryAndExitRequests(virtualMachine: VirtualMachine) {
        val manager = virtualMachine.eventRequestManager()

        val methodEntryReq = manager.createMethodEntryRequest()
        methodEntryReq.addClassFilter("TestKt")
        methodEntryReq.setSuspendPolicy(SUSPEND_ALL)
        methodEntryReq.enable()

        val methodExitReq = manager.createMethodExitRequest()
        methodExitReq.addClassFilter("TestKt")
        methodExitReq.setSuspendPolicy(SUSPEND_ALL)
        methodExitReq.enable()
    }

    private fun attachDebugger(port: Int): VirtualMachine {
        val connector = SocketAttachingConnector()
        val virtualMachine = connector.attach(connector.defaultArguments().toMutableMap().apply {
            getValue("port").setValue("$port")
            getValue("hostname").setValue("127.0.0.1")
        })
        return virtualMachine
    }

}

class SteppingDebugRunner(testServices: TestServices) : DebugRunner(testServices) {
    override fun storeStep(loggedItems: ArrayList<SteppingTestLoggedData>, event: Event) {
        assert(event is LocatableEvent)
        val location = (event as LocatableEvent).location()
        val data =
            if (isIndyLambda(location)) {
                // Invokedynamic lambdas are not synthetic in JDI, and they don't have source information.
                SteppingTestLoggedData(-1, true, "<lambda>")
            } else SteppingTestLoggedData(
                location.lineNumber(),
                location.method().isSynthetic,
                location.formatAsExpectation(null)
            )
        loggedItems.add(data)
    }
}

class LocalVariableDebugRunner(testServices: TestServices) : DebugRunner(testServices) {

    private fun toRecord(frame: StackFrame, variable: LocalVariable): LocalVariableRecord {
        val value = frame.getValue(variable)
        val valueRecord = if (value == null) {
            LocalNullValue
        } else if (value is ObjectReference && value.referenceType().name() != "java.lang.String") {
            LocalReference(value.uniqueID().toString(), value.referenceType().name())
        } else {
            LocalPrimitive(value.toString(), value.type().name())
        }
        return LocalVariableRecord(variable.name(), variable.typeName(), valueRecord)
    }

    private fun waitUntil(condition: () -> Boolean) {
        while (!condition()) {
            Thread.sleep(10)
        }
    }

    override fun storeStep(loggedItems: ArrayList<SteppingTestLoggedData>, event: Event) {
        val locatableEvent = event as LocatableEvent
        waitUntil { locatableEvent.thread().isSuspended }
        val location = locatableEvent.location()
        if (location.method().isSynthetic) return

        val frame = locatableEvent.thread().frame(0)
        val visibleVars = try {
            frame.visibleVariables().map { variable -> toRecord(frame, variable) }
        } catch (e: AbsentInformationException) {
            // Local variable table completely absent - not distinguished from an empty table.
            listOf()
        }
        val data =
            if (isIndyLambda(location)) {
                // Invokedynamic lambdas are not synthetic in JDI, and they don't have source information.
                SteppingTestLoggedData(-1, true, "<lambda>")
            } else SteppingTestLoggedData(
                location.lineNumber(),
                false,
                location.formatAsExpectation(visibleVars)
            )
        loggedItems.add(data)
    }
}

private fun isIndyLambda(location: Location): Boolean =
    "$\$Lambda$" in location.declaringType().name()
