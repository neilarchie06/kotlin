// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-65789

// FILE: Foo.kt
package bar

fun <T> take(arg: T): T = arg

fun Foo(string: String?) {}

class Foo(val str: String)

val foo = take<Foo>(Foo("1"))
val barFoo = take<Foo>(bar.Foo("2"))

// FILE: another.kt
package foo

import bar.Foo
import bar.take

val foo = take<Foo>(Foo("3"))

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter */
