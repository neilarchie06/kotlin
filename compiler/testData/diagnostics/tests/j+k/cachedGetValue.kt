// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.concurrent.atomic.AtomicReference

class CacheOwner<T>(
    private val compute: () -> T,
) {
    private val cached = AtomicReference<CachedValue<T>?>(null)

    public fun getValue(): T = cached.updateAndGet { value ->
        when {
            value == null -> createNewCachedValue()
            <!DEBUG_INFO_SMARTCAST!>value<!>.isUpToDate() -> value
            else -> createNewCachedValue()
        }
    }!!.value

    private fun createNewCachedValue() = CachedValue(compute())
}

private class CachedValue<T>(val value: T) {
    fun isUpToDate() = true
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, equalityExpression, flexibleType, functionDeclaration,
functionalType, javaFunction, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, samConversion,
smartcast, typeParameter, whenExpression */
