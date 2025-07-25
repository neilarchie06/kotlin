// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContractSyntaxV2
import kotlin.contracts.*

fun printStr(str: String?) contract [
    returns() implies (str != null)
] {
    require(str != null)
    println(str)
}

fun callExactlyOnce(block: () -> Int) contract [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    val num = block()
    println(num)
}

fun calculateNumber(block: () -> Int): Int contract [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    val num = block()
    return num
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, functionalType, localProperty, nullableType, propertyDeclaration, smartcast */
