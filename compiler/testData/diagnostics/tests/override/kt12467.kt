// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface A {
    fun test() {
    }
}

interface B : A {
    override fun test()
}

interface C : A

interface D : C, B

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class K<!> : D

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, override */
