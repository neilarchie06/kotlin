// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas

fun test() {
    while(true) {
        {block: () -> Unit -> block()}(
            {<!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>}
        )
    }
}

/* GENERATED_FIR_TAGS: break, functionDeclaration, functionalType, lambdaLiteral, whileLoop */
