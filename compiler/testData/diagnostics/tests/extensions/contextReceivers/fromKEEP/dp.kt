// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class View

context(View) val Int.dp get() = 42 * this

fun View.f() {
    123.dp
    with(123) {
        dp
    }
}

fun Int.g(v: View) {
    with(v) {
        dp
    }
}

fun h() {
    123.<!NO_CONTEXT_RECEIVER!>dp<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
lambdaLiteral, multiplicativeExpression, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, thisExpression */
