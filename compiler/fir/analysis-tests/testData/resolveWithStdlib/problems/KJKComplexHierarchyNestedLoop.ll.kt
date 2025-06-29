// LL_FIR_DIVERGENCE
// Behavior of specific diagnostics is undefined in presence of cycles in supertypes
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// FILE: K1.kt
class K2: <!CYCLIC_INHERITANCE_HIERARCHY!>J1<!>() {
    class Q : <!UNRESOLVED_REFERENCE!>Nested<!>()
    fun bar() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>baz<!>()

        <!UNRESOLVED_REFERENCE!>superClass<!>()
        <!UNRESOLVED_REFERENCE!>superI<!>()
    }
}

// FILE: J1.java
public class J1 extends K2 {
    public class Nested {}

    void baz() {}
}

// FILE: K2.kt
open class KFirst : SuperClass(), SuperI {
    fun foo() {
    }
}

// FILE: K3.kt
abstract class SuperClass {
    fun superClass() {}
}

interface SuperI {
    fun superI() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, javaType, nestedClass */
