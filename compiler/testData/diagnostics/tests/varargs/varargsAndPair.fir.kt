// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun <T: Any> foo(vararg ts: T): T? = null

class Pair<A>(a: A)

fun test() {
    val v = foo(Pair(1))
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>v<!>) // check that it is not error type
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter,
typeWithExtension, vararg */
