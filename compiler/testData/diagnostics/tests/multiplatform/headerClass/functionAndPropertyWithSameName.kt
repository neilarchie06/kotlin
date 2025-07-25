// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    val bar: String
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    actual val bar = "bar"
    fun bar() = bar
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, propertyDeclaration, stringLiteral */
