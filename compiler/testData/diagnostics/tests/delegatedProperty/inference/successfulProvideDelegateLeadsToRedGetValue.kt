// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-61077

val test: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!TYPE_MISMATCH!>materializeDelegate<!>()<!>

fun <T: CharSequence> materializeDelegate(): Box<T> = TODO()

operator fun <K: Comparable<K>> Box<K>.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): K = TODO()

operator fun <Q: Comparable<Q>> Q.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Q = TODO()

class Box<V> {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, intersectionType, nullableType,
operator, propertyDeclaration, propertyDelegate, starProjection, typeConstraint, typeParameter */
