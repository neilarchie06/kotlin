// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface ILeft {
    fun foo()
}

interface IRight {
    fun foo()
}

interface IDerived : ILeft, IRight

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class CDerived<!> : ILeft, IRight

abstract class ADerived : ILeft, IRight

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration */
