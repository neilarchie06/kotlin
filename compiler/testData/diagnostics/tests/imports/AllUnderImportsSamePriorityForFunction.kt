// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: a.kt
package a

fun X(p: Int) {}

// FILE: b.kt
package b

fun X(): Int = 1

// FILE: c.kt
package c

import b.*
import a.X

fun foo() {
    val v: Int = X()
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
