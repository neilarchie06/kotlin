// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
//KT-1078 Problem with visibility in do-while

package kt1078

fun test() : B {
    do {
        val x = foo()
    } while(x.bar()) // x is not visible here!
    return B()
}

class B() {
    fun bar() = true
}

fun foo() = B()

/* GENERATED_FIR_TAGS: classDeclaration, doWhileLoop, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration */
