// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE

fun test() {
    val x: Int? = 20
    if (x != null) {
    } else {
        if (true) return else return
    }
    <!DEBUG_INFO_SMARTCAST!>x<!>.and(1) // unsafe call
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, smartcast */
