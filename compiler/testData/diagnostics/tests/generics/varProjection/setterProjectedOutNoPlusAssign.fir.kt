// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNREACHABLE_CODE
interface Tr<T> {
    var v: T
}

fun test(t: Tr<out String>) {
    // resolved as t.v = t.v + null!!, where type of right operand is String,
    // so TYPE_MISMATCH: String is not <: of Captured(out String)
    t.<!SETTER_PROJECTED_OUT!>v<!> += null!!
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, capturedType, checkNotNullCall, functionDeclaration,
interfaceDeclaration, localProperty, nullableType, outProjection, propertyDeclaration, typeParameter */
