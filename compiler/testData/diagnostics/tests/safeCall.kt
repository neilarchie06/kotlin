// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

fun f(s: String, action: (String.() -> Unit)?) {
    s.foo().bar().<!UNSAFE_IMPLICIT_INVOKE_CALL!>action<!>()
}

fun String.foo() = ""

fun String.bar() = ""

// --------------------------------------------------------

val functions: Map<String, () -> Any> = TODO()

fun run(name: String) = <!UNSAFE_IMPLICIT_INVOKE_CALL!>functions[name]<!>()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, nullableType, propertyDeclaration,
stringLiteral, typeWithExtension */
