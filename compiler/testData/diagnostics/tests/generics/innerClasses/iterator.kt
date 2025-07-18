// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_VARIABLE
// JAVAC_EXPECTED_FILE
import java.util.*

class A<T> : AbstractCollection<T>() {
    override fun iterator(): MyIt = MyIt()

    override val size: Int
        get() = 1

    inner class MyIt : MutableIterator<T> {
        override fun next(): T {
            throw UnsupportedOperationException()
        }

        override fun hasNext(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }
}

fun <E> commonSupertype(x: E, y: E): E = x

fun foo() {
    var myIt = A<String>().iterator()
    myIt = <!TYPE_MISMATCH!>A<Int>().iterator()<!>

    val csIt: Iterator<CharSequence> = A<String>().iterator()

    commonSupertype(A<String>().iterator(), A<Int>().iterator()).checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><A<out Any>.MyIt>() }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
getter, infix, inner, integerLiteral, intersectionType, lambdaLiteral, localProperty, nullableType, operator,
outProjection, override, propertyDeclaration, typeParameter, typeWithExtension */
