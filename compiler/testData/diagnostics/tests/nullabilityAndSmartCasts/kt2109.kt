// RUN_PIPELINE_TILL: BACKEND
//KT-2109 Nullability inference fails in extension function
package kt2109

class A {
    fun foo() {}
}

fun A?.bar() {
    if (this == null) {
        return
    }
    <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>()
}

fun A.baz() {
    if (<!SENSELESS_COMPARISON!>this == null<!>) {
        return
    }
    foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
nullableType, smartcast, thisExpression */
