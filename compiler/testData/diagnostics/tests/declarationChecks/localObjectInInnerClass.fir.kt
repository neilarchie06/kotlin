// RUN_PIPELINE_TILL: FRONTEND
interface I1
interface I2

interface Lazy<T> {
    operator fun getValue(a1: Any, a2: Any): T
}

fun <T> lazy(f: () -> T): Lazy<T> = throw Exception()

class A {
    private inner class B {
        val o1 = object : I1 {}
        val o2 by lazy {
            object : I1 {}
        }
        val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>o3<!> = object : I1, I2 {} // FIR allows this since the containing class is private
        val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>o4<!> by lazy { // FIR allows this since the containing class is private
            object : I1, I2 {}
        }

        private val privateO1 = object : I1 {}
        private val privateO2 by lazy {
            object : I1 {}
        }
        private val privateO3 = object : I1, I2 {}
        private val privateO4 by lazy {
            object : I1, I2 {}
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, functionalType, inner,
interfaceDeclaration, lambdaLiteral, nullableType, operator, propertyDeclaration, propertyDelegate, typeParameter */
