// RUN_PIPELINE_TILL: FRONTEND
// FILE: KotlinFile.kt

fun foo(o: JavaClass2) {
    useString(o.something)
    o.something = ""
    o.setSomething(1)
    o.something = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> // we generate extension property for JavaClass2 with more specific type
    o.something += "1"
}

fun useString(i: String) {}

// FILE: JavaClass1.java
public class JavaClass1 {
    public Object getSomething() { return null; }
    public void setSomething(Object value) { }
}

// FILE: JavaClass2.java
public class JavaClass2 extends JavaClass1 {
    public String getSomething() { return ""; }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, flexibleType, functionDeclaration, integerLiteral, javaFunction,
javaProperty, javaType, localProperty, propertyDeclaration, stringLiteral */
