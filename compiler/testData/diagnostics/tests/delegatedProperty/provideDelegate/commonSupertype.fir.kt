// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-61781

fun TODO(): Nothing = null!!

class MyPair<out A, out B>
fun <X : Delegate<Y>, Y> mat(): MyPair<X, Delegate<Y>> = TODO()

operator fun <T> MyPair<T, T>.provideDelegate(a: Any?, b: Any?): T = TODO()

interface Delegate<V> {
    operator fun getValue(a: Any?, b: Any?): V = TODO()
}

fun main() {
    val x: String by mat()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
interfaceDeclaration, localProperty, nullableType, operator, out, propertyDeclaration, propertyDelegate, typeConstraint,
typeParameter */
