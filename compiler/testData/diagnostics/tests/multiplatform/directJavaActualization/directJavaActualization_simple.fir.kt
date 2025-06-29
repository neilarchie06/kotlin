// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
open class Base() {
    open fun fakeOverrideInExpect() {}
}

expect open class Foo() : Base {
    fun foo()
    open fun fakeOverrideInActual()

    class Nested()
    inner class Inner()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo extends Base implements JavaBase {
    @kotlin.annotations.jvm.KotlinActual public Foo() {}
    @kotlin.annotations.jvm.KotlinActual public void foo() {}

    @Override
    public void fakeOverrideInExpect() {}

    public void additionalMember() {}

    @kotlin.annotations.jvm.KotlinActual public static class Nested {
        @kotlin.annotations.jvm.KotlinActual public Nested() {}
    }
    @kotlin.annotations.jvm.KotlinActual public class Inner {
        @kotlin.annotations.jvm.KotlinActual public Inner() {}
    }
}

// FILE: JavaBase.java
public interface JavaBase {
    default void fakeOverrideInActual() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, expect, functionDeclaration, inner, nestedClass, primaryConstructor */
