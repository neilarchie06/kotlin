// RUN_PIPELINE_TILL: BACKEND
// See KT-20959

enum class Foo {;
    companion object  {
        val x = <!DEBUG_INFO_LEAKING_THIS!>foo<!>() // there should be no UNINITIALIZED_ENUM_COMPANION

        private fun foo() = "OK"
    }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, functionDeclaration, objectDeclaration, propertyDeclaration,
stringLiteral */
