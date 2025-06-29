// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(a: Int = 1)

    class Nested {
        constructor(b: Int = 2)
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo {
    @kotlin.annotations.jvm.KotlinActual public void foo(int a) {
    }

    @kotlin.annotations.jvm.KotlinActual public static class Nested {
        @kotlin.annotations.jvm.KotlinActual public Nested(int b) {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, expect, functionDeclaration, integerLiteral, nestedClass, secondaryConstructor */
