// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-54764

data class Out<out T>(val prop: T)

fun foo(b: Out<*>) {
    b.copy("") // error in K1, OK in K2
}

fun foo(a: Any) {
    if (a is Out<*>) {
        a.copy("")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, ifExpression, isExpression, nullableType, out,
primaryConstructor, propertyDeclaration, smartcast, starProjection, stringLiteral, typeParameter */
