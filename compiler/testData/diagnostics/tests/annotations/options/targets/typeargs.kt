// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class base

val x: List<<!WRONG_ANNOTATION_TARGET!>@base<!> String>? = null

val y: List<@[<!WRONG_ANNOTATION_TARGET!>base<!>] String>? = null

@Target(AnnotationTarget.TYPE)
annotation class typeAnn

fun foo(list: List<@typeAnn Int>) = list

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, nullableType, propertyDeclaration */
