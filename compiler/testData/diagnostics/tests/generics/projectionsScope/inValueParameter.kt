// RUN_PIPELINE_TILL: FRONTEND
interface A<T>
interface B<E> {
    fun foo(x: A<in E>)
}

fun foo(x: B<in CharSequence>, y: A<CharSequence>) {
    x.foo(<!TYPE_MISMATCH!>y<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, interfaceDeclaration, nullableType, typeParameter */
