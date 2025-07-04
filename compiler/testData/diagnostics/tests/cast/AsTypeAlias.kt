// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
typealias MyString = String

val x: MyString = ""
val y = x as Any

interface Base
class Derived : Base
interface Other : Base
typealias IBase = Base
typealias IOther = Other

val ib: IBase = Derived()
val d = ib as Derived
val o = ib as Other
val io = ib as IOther
val s = d <!CAST_NEVER_SUCCEEDS!>as<!> String
val ms = d <!CAST_NEVER_SUCCEEDS!>as<!> MyString

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, interfaceDeclaration, propertyDeclaration, stringLiteral,
typeAliasDeclaration */
