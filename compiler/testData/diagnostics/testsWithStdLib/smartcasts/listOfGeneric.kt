// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// See KT-9529 Smart cast causes code to be incompilable

fun <T : Any> foo(o: T): Collection<T> {
    if (o is String) {
        return listOf(o)
    }
    return listOf(o)
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, isExpression, smartcast, typeConstraint,
typeParameter */
