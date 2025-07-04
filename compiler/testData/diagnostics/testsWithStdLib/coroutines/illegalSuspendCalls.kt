// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
import Host.bar

object Host {
    suspend fun bar() {}
}

suspend fun foo() {}

fun noSuspend() {
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>foo<!>()
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>bar<!>()
}

class A {
    init {
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>foo<!>()
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>bar<!>()
    }
}

val x = <!ILLEGAL_SUSPEND_FUNCTION_CALL!>foo<!>()
val y = <!ILLEGAL_SUSPEND_FUNCTION_CALL!>bar<!>()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, init, objectDeclaration, propertyDeclaration, suspend */
