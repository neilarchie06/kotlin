// RUN_PIPELINE_TILL: BACKEND
package test

val NAMED_CONSTANT = 1

// val prop1: false
val prop1 = NAMED_CONSTANT

// val prop2: false
val prop2 = NAMED_CONSTANT + 1

/* GENERATED_FIR_TAGS: additiveExpression, integerLiteral, propertyDeclaration */
