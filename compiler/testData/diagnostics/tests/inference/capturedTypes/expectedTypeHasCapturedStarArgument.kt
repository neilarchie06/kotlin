// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
interface A<V>

fun interface F<O> {
  fun apply(): A<O>
}

object C {
  fun <V> createA(): A<V> = TODO()
}

class B<V> {
  fun bar(function: F<out V>): B<V> = TODO()

  companion object {
    fun <X> from(a: A<X>): B<X> = TODO()
  }
}

fun foo(a: A<*>) {
  B.from(a).bar { C.createA() }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, companionObject, funInterface, functionDeclaration,
interfaceDeclaration, lambdaLiteral, nullableType, objectDeclaration, outProjection, samConversion, starProjection,
typeParameter */
