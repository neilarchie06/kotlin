// RUN_PIPELINE_TILL: BACKEND
interface UpdatableRendering<out T : UpdatableRendering<T>> {
    fun canUpdateFrom(another: @UnsafeVariance T): Boolean
}

internal fun Any.matchesRendering(other: Any): Boolean {
    return when {
        this::class != other::class -> false
        this !is UpdatableRendering<*> -> true
        else -> <!DEBUG_INFO_SMARTCAST!>this<!>.canUpdateFrom(other as UpdatableRendering<*>)
    }
}

/* GENERATED_FIR_TAGS: asExpression, classReference, equalityExpression, funWithExtensionReceiver, functionDeclaration,
interfaceDeclaration, isExpression, out, smartcast, starProjection, thisExpression, typeConstraint, typeParameter,
whenExpression */
