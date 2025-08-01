// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface Base {
    fun foo()
}

expect <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>object Implementation<!> : Base

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

abstract class RealImplementation : Base {
    override fun foo() {}
}

actual object Implementation : RealImplementation(), Base

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, interfaceDeclaration, objectDeclaration,
override */
