// RUN_PIPELINE_TILL: FRONTEND
interface I<F, G>

val aImpl: A.Companion.Interface
    get() = null!!

val bImpl: B.Companion.Interface
    get() = null!!

interface A {
    companion object : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE!>Interface<!> by aImpl, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>> {

        class Nested

        interface Interface
    }
}

class B {
    companion object : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE!>Interface<!> by aImpl, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>> {

        class Nested

        interface Interface
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, companionObject, getter, inheritanceDelegation,
interfaceDeclaration, nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeParameter */
