// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
<!STRICTFP_ON_CLASS!>@kotlin.jvm.Strictfp<!> class A {

}

<!STRICTFP_ON_CLASS!>@kotlin.jvm.Strictfp<!> object B {

}

<!STRICTFP_ON_CLASS!>@kotlin.jvm.Strictfp<!> interface C {

}

fun foo() {
    <!STRICTFP_ON_CLASS!>@kotlin.jvm.Strictfp<!> class D

    <!STRICTFP_ON_CLASS!>@kotlin.jvm.Strictfp<!> object: Any() {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration,
localClass, objectDeclaration */
