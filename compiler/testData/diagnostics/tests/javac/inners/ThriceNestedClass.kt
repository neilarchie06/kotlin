// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: a/x.java
package a;

public class x {

    public b getB() { return null; }

    public static class b {

        public b getB() { return null; }

        public static class b {

            public b getB() { return null; }

            public static class b {
                public b getB() { return null; }
            }

        }

    }

}

// FILE: test.kt
package a

fun test() = x().getB()
fun test2() = test().getB()
fun test3() = test2().getB()
fun test4() = test3().getB()

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
