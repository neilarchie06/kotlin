// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED, -CONTEXT_CLASS_OR_CONSTRUCTOR
// LANGUAGE: +ContextReceivers, +ValueClasses
// WITH_STDLIB
// SKIP_TXT
// WORKS_WHEN_VALUE_CLASS
// DONT_WARN_ON_ERROR_SUPPRESSION

@file:Suppress("INLINE_CLASS_DEPRECATED")

class A

<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(A)<!>
inline class B1(val x: Int)

<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(A)<!>
OPTIONAL_JVM_INLINE_ANNOTATION
value class B2(val x: Int)

<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(A)<!>
OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val x: Int, val y: Int)

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral, value */
