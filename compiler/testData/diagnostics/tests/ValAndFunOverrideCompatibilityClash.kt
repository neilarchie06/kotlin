// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE
class Foo1() : java.util.ArrayList<Int>()

open class Bar() {
    fun v() : Int  = 1
    val v : Int = 1
}

class Barr() : Bar() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
