// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// Issue: KT-36044

fun <A> select(x: A, f: () -> A) = f()
fun <B> map(f: () -> B) = f()

fun main() {
    select('a', map { { "" } })
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, intersectionType, lambdaLiteral, nullableType, stringLiteral,
typeParameter */
