// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// NI_EXPECTED_FILE

val test1 = { when (true) { true -> 1; else -> "" } }

val test2 = { { when (true) { true -> 1; else -> "" } } }

val test3: (Boolean) -> Any = { when (true) { true -> 1; else -> "" } }

val test4: (Boolean) -> Any? = { when (true) { true -> 1; else -> "" } }

fun println() {}

val test5 = {
    when (true) {
        true -> println()
        else -> println()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, integerLiteral, intersectionType,
lambdaLiteral, nullableType, propertyDeclaration, stringLiteral, whenExpression, whenWithSubject */
