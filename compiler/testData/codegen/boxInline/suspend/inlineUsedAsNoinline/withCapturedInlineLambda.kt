// WITH_STDLIB
// WITH_REFLECT
// WITH_COROUTINES
// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// FILE: inlined.kt
package flow

interface Flow {
    suspend fun collect(collector: FlowCollector)
}

interface FlowCollector {
    suspend fun emit(value: String)
}

inline fun flow(crossinline body: suspend FlowCollector.() -> Unit): Flow =
    object : Flow {
        override suspend fun collect(collector: FlowCollector) = collector.body()
    }

suspend inline fun Flow.collect(crossinline body: suspend (String) -> Unit) =
    collect(object : FlowCollector {
        override suspend fun emit(value: String) = body(value)
    })

inline fun Flow.transform(crossinline transformer: suspend FlowCollector.(value: String) -> Unit): Flow =
    flow { collect { value -> transformer(value) } }

inline fun Flow.map(crossinline transformer: suspend (value: String) -> String): Flow =
    transform { value -> emit(transformer(value)) }

// FILE: box.kt
import flow.*
import helpers.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun addK(x: String): String = suspendCoroutineUninterceptedOrReturn { cont ->
    cont.resume(x + "K")
    COROUTINE_SUSPENDED
}

var result = ""

fun box(): String {
    suspend {
        val source = flow { emit("O") }
        val reference: (suspend (String) -> String) -> Flow = source::map
        ((reference as KFunction<*>).javaMethod!!.invoke(null, source, ::addK) as Flow).collect { result = it }
    }.startCoroutine(EmptyContinuation)
    return result
}
