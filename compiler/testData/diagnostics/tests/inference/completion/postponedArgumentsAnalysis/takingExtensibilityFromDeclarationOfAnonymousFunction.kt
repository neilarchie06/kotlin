// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER

fun <T> id(x: T) = x
fun <T> select(vararg x: T) = x[0]

val x1 = select(id { this }, fun Int.() = this)
val x2 = select(id { this + it.inv() }, fun Int.(x: Int) = this)
val x3 = select(id { this.length + it.inv() }, fun String.(x: Int) = length)

/* GENERATED_FIR_TAGS: additiveExpression, anonymousFunction, capturedType, functionDeclaration, integerLiteral,
lambdaLiteral, nullableType, outProjection, propertyDeclaration, thisExpression, typeParameter, vararg */
