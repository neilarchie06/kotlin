// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A {
    fun foo() {}
}

fun A?.foo() {}

val f: KFunction1<A, Unit> = A::foo
val g: KFunction1<A, Unit> = A?::foo

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType,
propertyDeclaration */
