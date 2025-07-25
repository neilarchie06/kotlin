// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNNECESSARY_NOT_NULL_ASSERTION
// Advancement of KT-9126

class My(val x: Int?) {
    operator fun plus(y: My) = if (this.x != null) this else y
}

fun foo() {
    var y: My? = My(42)
    if (y!!.x != null) {
        y = My(null)
        (<!DEBUG_INFO_SMARTCAST!>y<!> + My(0)).x<!UNSAFE_CALL!>.<!>hashCode()
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, checkNotNullCall, classDeclaration, equalityExpression,
functionDeclaration, ifExpression, integerLiteral, localProperty, nullableType, operator, primaryConstructor,
propertyDeclaration, smartcast, thisExpression */
