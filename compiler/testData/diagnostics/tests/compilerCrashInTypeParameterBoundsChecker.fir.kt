// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64644
// WITH_STDLIB

typealias MaybePair = Pair<Int, Int>?

fun <T: MaybePair> foo(x: T) {
    if (x != null) {
        println(x.first)
        println(x.second)
    }
}

/* GENERATED_FIR_TAGS: dnnType, equalityExpression, functionDeclaration, ifExpression, nullableType, smartcast,
typeAliasDeclaration, typeConstraint, typeParameter */
