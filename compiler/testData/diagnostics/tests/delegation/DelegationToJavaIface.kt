// RUN_PIPELINE_TILL: FRONTEND
// JAVAC_EXPECTED_FILE
// WITH_EXTRA_CHECKERS

class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>) : <!DELEGATION_NOT_TO_INTERFACE, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!> by o {}

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, primaryConstructor */
