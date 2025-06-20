// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// KT-12751 Type inference failed with forEach and bound reference

class L<out T>

fun <T> L<T>.foo(action: (T) -> Unit): Unit {}

class B {
    fun remove(charSequence: CharSequence) {}
}

fun foo(list: L<CharSequence>, b: B) {
    list.foo(b::remove)
    list.foo<CharSequence>(b::remove)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, nullableType, out, typeParameter */
