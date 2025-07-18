// RUN_PIPELINE_TILL: FRONTEND

// FILE: Sam.java

public interface Sam<K> {
    Sam.Result<K> compute();

    public static class Result<V> {
        public static <V> Sam.Result<V> create(V value) {}
    }
}

// FILE: Foo.java

public class Foo {
    public static <T> void foo(Sam<T> var1) {
    }
}

// FILE: test.kt

fun test(e: <!UNRESOLVED_REFERENCE!>ErrorType<!>) {
    Foo.<!CANNOT_INFER_PARAMETER_TYPE!>foo<!> {
        Sam.Result.<!CANNOT_INFER_PARAMETER_TYPE!>create<!>(e)
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, samConversion */
