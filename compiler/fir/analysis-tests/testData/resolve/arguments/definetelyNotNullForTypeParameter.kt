// RUN_PIPELINE_TILL: BACKEND
interface Out<out E>

fun <X> id(x: Out<X>): Out<X> = TODO()

fun <F : Any> foo(computable: Out<F?>) {}

fun <T : Any> bar(computable: Out<T?>) {
    foo(id(computable))
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, out, typeConstraint, typeParameter */
