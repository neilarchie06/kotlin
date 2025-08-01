// RUN_PIPELINE_TILL: BACKEND
class Generic<T : CharSequence?>(val value: T) {
    fun foo(): T = value
}

fun test(arg: Generic<String>) {
    val value = arg.value
    val foo = arg.foo()
    val length = foo.length + value.length
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
