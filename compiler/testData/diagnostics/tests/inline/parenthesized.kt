// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

inline fun inlineFunWithInvoke(s: (p: Int) -> Unit) {
    (s)(11)
    (s).invoke(11)
    (s) <!INFIX_MODIFIER_REQUIRED!>invoke<!> 11
    (<!USAGE_IS_NOT_INLINABLE!>s<!>)
}

<!NOTHING_TO_INLINE!>inline<!> fun Function1<Int, Unit>.inlineExt() {
    (this).invoke(11)
    (this) <!INFIX_MODIFIER_REQUIRED!>invoke<!> 11
    (this)(11)
    (this)
}

inline fun inlineFunWithInvoke2(s: (p: Int) -> Unit) {
    (((s)))(11)
    (((s))).invoke(11)
    (((s))) <!INFIX_MODIFIER_REQUIRED!>invoke<!> 11
    (((<!USAGE_IS_NOT_INLINABLE!>s<!>)))
}

inline fun propagation(s: (p: Int) -> Unit) {
    inlineFunWithInvoke((<!REDUNDANT_LABEL_WARNING!>label@<!> s))
    inlineFunWithInvoke((<!REDUNDANT_LABEL_WARNING!>label2@<!> <!REDUNDANT_LABEL_WARNING!>label@<!> s))
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix, inline,
integerLiteral, nullableType, thisExpression, typeParameter, typeWithExtension */
