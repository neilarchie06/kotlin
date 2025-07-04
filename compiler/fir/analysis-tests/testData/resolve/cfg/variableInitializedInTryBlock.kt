// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
// ISSUE: KT-48376

fun test() {
    val b: Boolean
    try {
        val s = getStringOrNull() ?: return
        b = s.length != 0
    } finally {
        test()
    }
    takeBoolean(b)
}

fun takeBoolean(b: Boolean) {}

fun getStringOrNull(): String? {
    return "hello"
}

/* GENERATED_FIR_TAGS: assignment, elvisExpression, equalityExpression, functionDeclaration, integerLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral, tryExpression */
