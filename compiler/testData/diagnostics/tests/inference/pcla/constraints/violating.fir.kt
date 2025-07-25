// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB

interface A {
    fun foo(): MutableList<String>
}

@ExperimentalStdlibApi
fun main() {
    buildList {
        add(3)
        object : A {
            override fun foo(): MutableList<String> = <!RETURN_TYPE_MISMATCH!>this@buildList<!>
        }
    }
    buildList {
        add(3)
        val x: String = <!INITIALIZER_TYPE_MISMATCH!>get(0)<!>
    }
    buildList {
        add("3")
        val x: MutableList<Int> = <!INITIALIZER_TYPE_MISMATCH!>this@buildList<!>
    }
    buildList {
        val y: CharSequence = ""
        add(y)
        val x: MutableList<String> = <!INITIALIZER_TYPE_MISMATCH!>this@buildList<!>
    }
    buildList {
        add("")
        val x: StringBuilder = <!INITIALIZER_TYPE_MISMATCH!>get(0)<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, integerLiteral, interfaceDeclaration,
lambdaLiteral, localProperty, override, propertyDeclaration, stringLiteral, thisExpression */
