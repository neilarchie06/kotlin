// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(f: () -> Collection<T>, p: (T) -> Boolean): Collection<T> = throw Exception()

fun <T> emptyList(): List<T> = throw Exception()

fun test(): Collection<Int> = foo({ emptyList<Int>() }, { x -> x > 0 })

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, typeParameter */
