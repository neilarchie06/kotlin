// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class special

@Target(AnnotationTarget.TYPE)
annotation class base

fun transform(i: Int, tr: (<!WRONG_ANNOTATION_TARGET!>@special<!> Int) -> Int): Int = @special tr(@special i)

fun foo(i: Int): Int {
    val j = @special i + 1
    if (j == 1) return foo(@special 42)
    return transform(@special j, @special { i: @base Int -> <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!><!WRONG_ANNOTATION_TARGET!>@base<!> i * 2<!> })
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, equalityExpression, functionDeclaration,
functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty, multiplicativeExpression,
propertyDeclaration */
