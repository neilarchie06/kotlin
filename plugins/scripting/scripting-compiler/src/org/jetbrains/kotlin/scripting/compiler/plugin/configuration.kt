/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.loadScriptTemplatesFromClasspath
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

const val KOTLIN_SCRIPTING_PLUGIN_ID = "kotlin.scripting"

fun configureScriptDefinitions(
    scriptTemplates: List<String>,
    configuration: CompilerConfiguration,
    baseClassloader: ClassLoader,
    messageCollector: MessageCollector,
    hostConfiguration: ScriptingHostConfiguration
) {
    // TODO: consider using escaping to allow kotlin escaped names in class names
    val templatesFromClasspath = loadScriptTemplatesFromClasspath(
        scriptTemplates, configuration.jvmClasspathRoots, emptyList(), baseClassloader, hostConfiguration, messageCollector.reporter
    )
    configuration.addAll(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, templatesFromClasspath.toList())
}

val ScriptingHostConfigurationKeys.configureFirSession by PropertiesCollection.key<FirSession.() -> Unit>(isTransient = true)
