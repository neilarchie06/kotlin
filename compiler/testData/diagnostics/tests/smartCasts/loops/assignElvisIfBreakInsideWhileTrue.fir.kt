// RUN_PIPELINE_TILL: FRONTEND
public fun foo(x: String?, y: String?): Int {
    while (true) {
        val z = x ?: if (y == null) break else y
        // z is not null in both branches
        z.length
        // y is nullable if x != null
        y<!UNSAFE_CALL!>.<!>length
    }
    // y is null because of the break
    return y<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: break, elvisExpression, equalityExpression, functionDeclaration, ifExpression, localProperty,
nullableType, propertyDeclaration, smartcast, whileLoop */
