// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-64625
// DIAGNOSTICS: -DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE

interface A<T1, T2>
interface B<X> : A<X, <!UNRESOLVED_REFERENCE!>T<!>?>

fun f(x: A<Int, <!UNRESOLVED_REFERENCE!>K<!>?>) = x as B

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
