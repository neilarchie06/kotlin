// RUN_PIPELINE_TILL: FRONTEND
// FILE:a.kt
package a

<!SYNTAX!><<!><!SYNTAX!><<!><!SYNTAX!><<!> <!SYNTAX!>FOOO<!><!SYNTAX!><!>
import b.B        //class
import b.foo      //function
import b.ext      //extension function
import b.value    //property
import b.C.Companion.bar    //function from companion object
import b.C.Companion.cValue //property from companion object
import b.<!UNRESOLVED_IMPORT!>constant<!>.fff     //function from val
import b.<!UNRESOLVED_IMPORT!>constant<!>.dValue  //property from val
import <!UNRESOLVED_IMPORT!>smth<!>.illegal
import b.C.<!UNRESOLVED_IMPORT!>smth<!>.illegal

<!SYNTAX!><<!><!SYNTAX!><<!><!SYNTAX!><<!><!SYNTAX!>HEAD<!><!SYNTAX!><!>
import b.<!UNRESOLVED_IMPORT!>bar<!>.smth
import b.<!UNRESOLVED_IMPORT!>bar<!>.*
import b.<!UNRESOLVED_IMPORT!>unr<!>.unr.unr
import <!UNRESOLVED_IMPORT!>unr<!>.unr.unr
import b.constant
import b.E.Companion.f      //val from companion object

fun test(arg: B) {
    foo(value)
    arg.ext()

    bar()
    foo(cValue)

    <!UNRESOLVED_REFERENCE!>fff<!>(<!UNRESOLVED_REFERENCE!>dValue<!>)

    constant.fff(constant.dValue)

    f.f()
}

// FILE:b.kt
package b

class B() {}

fun foo(i: Int) = i

fun B.ext() {}

val value = 0

class C() {
    companion object {
        fun bar() {}
        val cValue = 1
    }
}

class D() {
    fun fff(s: String) = s
    val dValue = "w"
}

val constant = D()

class E() {
    companion object {
        val f = F()
    }
}

class F() {
    fun f() {}
}

fun bar() {}

//FILE:c.kt
package c

import c.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>C<!>.*

object C {
    fun f() {
    }
    val i = 348
}

fun foo() {
    if (i == 3) f()
}

//FILE:d.kt
package d

import d.A.Companion.B
import d.A.Companion.C

val b : B = B()
val c : B = C

class A() {
    companion object {
        open class B() {}
        object C : B() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, integerLiteral, nestedClass, objectDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
