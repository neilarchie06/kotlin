// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt
package a

class X

// FILE: b.kt
package b

class X

// FILE: c.kt
package c

import a.<!CONFLICTING_IMPORT!>X<!>
import b.<!CONFLICTING_IMPORT!>X<!>

class Y : <!OVERLOAD_RESOLUTION_AMBIGUITY!>X<!>

/* GENERATED_FIR_TAGS: classDeclaration */
