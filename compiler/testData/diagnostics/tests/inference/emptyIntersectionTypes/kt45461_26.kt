// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>): S = null <!UNCHECKED_CAST!>as S<!>
}

class Out<out K>

fun <K : L, L : N, N: <!FINAL_UPPER_BOUND!>Out<Int><!>> main() {
    val foo = Foo<K>()
    val x: Out<Float> = <!TYPE_MISMATCH!>Bar<Out<String>>().<!TYPE_MISMATCH!>takeFoo<!>(foo)<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, inProjection, intersectionType,
localProperty, nullableType, out, propertyDeclaration, typeConstraint, typeParameter */
