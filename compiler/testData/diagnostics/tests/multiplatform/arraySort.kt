// FIR_IDENTICAL
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

public expect fun <T : Comparable<T>> Array<out T>.sort(fromIndex: Int = 0, toIndex: Int = size): Unit

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
public actual fun <T : Comparable<T>> Array<out T>.sort(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>fromIndex: Int = 0<!>, <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>toIndex: Int = size<!>): Unit {
}

public fun <T> Array<out T>.sort(fromIndex: Int = 0, toIndex: Int = size): Unit {
}

/* GENERATED_FIR_TAGS: actual, expect, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
outProjection, typeConstraint, typeParameter */
