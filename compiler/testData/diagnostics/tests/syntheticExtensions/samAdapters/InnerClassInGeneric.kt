// RUN_PIPELINE_TILL: FRONTEND
// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<Int>): Int {
    val inner = javaClass.createInner<String>()
    return <!TYPE_MISMATCH!>inner.doSomething(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, "") { }<!>
}

// FILE: JavaClass.java
public class JavaClass<T> {
    public <X> Inner<X> createInner() {
        return new Inner<X>();
    }

    public interface Inner<X>{
        public T doSomething(T t, X x, Runnable runnable);
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaType, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, stringLiteral */
