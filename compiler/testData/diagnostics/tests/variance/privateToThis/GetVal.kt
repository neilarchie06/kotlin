// RUN_PIPELINE_TILL: FRONTEND
fun <T> getT(): T = null!!

class Test<in I, out O> {
    private val i: I = getT()

    init {
        apply(i)
        apply(this.i)
    }

    fun apply(i: I) {}

    fun test() {
        apply(i)
        apply(this.i)
        with(Test<I, O>()) {
            apply(i) // K1: this@Test.i, K2: this@with.i, see KT-55446
            apply(this.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
            apply(this@with.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
            apply(this@Test.i)
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.apply(t.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
    }

    companion object {
        fun <I, O> test(t: Test<I, O>) {
            t.apply(t.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.apply(t.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, companionObject, functionDeclaration, in, init, lambdaLiteral,
nullableType, objectDeclaration, out, propertyDeclaration, thisExpression, typeParameter */
