// WITH_COROUTINES
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// MODULE: lib
// FILE: a.kt
private suspend fun privateSuspendMethod() = "OK"

internal suspend inline fun internalInline() = privateSuspendMethod()

// MODULE: main()(lib)
// FILE: main.kt
import kotlin.coroutines.*

fun runBlocking(c: suspend () -> String): String {
    var res: String? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

fun box(): String = runBlocking {
    return@runBlocking internalInline()
}