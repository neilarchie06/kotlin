// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
fun foo(vararg t : String) = ""
fun foo(vararg t : Int) = ""

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral, vararg */
