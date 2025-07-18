// RUN_PIPELINE_TILL: FRONTEND
class B

class A {
    operator fun B.invoke() {}
}

val B.a: () -> Int  get() = { 5 }

fun test(a: A, b: B) {
    val x: Int = b.a()

    b.<!FUNCTION_EXPECTED!>(a)<!>()

    with(b) {
        val y: Int = a()
        <!FUNCTION_EXPECTED!>(a)<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, getter,
integerLiteral, lambdaLiteral, localProperty, operator, propertyDeclaration, propertyWithExtensionReceiver */
