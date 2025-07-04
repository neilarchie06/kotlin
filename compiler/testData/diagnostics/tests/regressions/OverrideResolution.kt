// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun box(c : C) {
    val a : C = c
    a.foo()
}

open class A {
    open fun foo() {}
}

open class B : A() {
    override fun foo() {}
}

open class C : B() {
    override fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, override, propertyDeclaration */
