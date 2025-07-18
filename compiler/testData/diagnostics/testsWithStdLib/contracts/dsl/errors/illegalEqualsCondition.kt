// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun equalsWithVariables(x: Any?, y: Any?) {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only equality comparisons with 'null' allowed")!>x == y<!>)
    }
}

fun identityEqualsWithVariables(x: Any?, y: Any?) {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only equality comparisons with 'null' allowed")!>x === y<!>)
    }
}

fun equalConstants() {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only equality comparisons with 'null' allowed"), SENSELESS_COMPARISON!>null == null<!>)
    }
}

fun get(): Int? = null
fun equalNullWithCall() {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only references to parameters are allowed in contract description")!>get()<!> == null)
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, equalityExpression, functionDeclaration, lambdaLiteral,
nullableType */
