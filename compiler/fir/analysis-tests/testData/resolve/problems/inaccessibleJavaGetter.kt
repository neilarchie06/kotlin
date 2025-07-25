// RUN_PIPELINE_TILL: FRONTEND
// FILE: PropertyDescriptor.java

public interface PropertyDescriptor extends DescriptorWithAccessor {
    String getSetter();
    boolean isDelegated();
}

// FILE: test.kt

interface DescriptorWithAccessor {
    val setter: String
    val isDelegated: Boolean
}

class WrappedPropertyDescriptor : PropertyDescriptor {
    override val setter: String get() = "K"
    override val isDelegated: Boolean get() = false
}

fun test() {
    val descriptor = WrappedPropertyDescriptor()
    val res1 = descriptor.setter
    val res2 = descriptor.<!UNRESOLVED_REFERENCE!>getSetter<!>() // Should be error
    val res3 = descriptor.isDelegated
    val res4 = descriptor.<!FUNCTION_EXPECTED!>isDelegated<!>() // Should be error
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, interfaceDeclaration, javaType, localProperty,
override, propertyDeclaration, stringLiteral */
