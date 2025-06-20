// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST

class Foo {
    suspend operator fun <T> invoke(body: () -> T) = null as T
}

suspend fun main() {
    val retry = Foo()
    try {
        retry { 1 } // Before the fix: "Suspend function 'invoke' should be called only from a coroutine or another suspend function"
    } catch (e: Exception) { }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration, suspend, tryExpression, typeParameter */
