// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

operator fun String.invoke(i: Int) {}

fun foo(s: String?) {
    <!UNSAFE_CALL!>s<!>(1)

    <!UNSAFE_IMPLICIT_INVOKE_CALL!>(s <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)<!>(1)
}

/* GENERATED_FIR_TAGS: elvisExpression, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
operator */
