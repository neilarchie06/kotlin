// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-66148

interface Controller<E>

fun <E1> generate(block: Controller<E1>.() -> Unit) {}

class A

fun foo(c: Controller<String>): A = TODO()

fun bar(
    propertyForInvoke: A.() -> Unit,
) {
    generate {
        foo(this).propertyForInvoke()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, thisExpression, typeParameter, typeWithExtension */
