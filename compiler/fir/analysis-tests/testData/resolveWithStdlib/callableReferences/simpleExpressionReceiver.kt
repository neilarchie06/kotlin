// RUN_PIPELINE_TILL: FRONTEND
class A {
    fun bar(x: String): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}

fun foo(x: (String) -> Int) {}

fun main() {
    val a = A()
    foo(a::bar)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, localProperty,
propertyDeclaration */
