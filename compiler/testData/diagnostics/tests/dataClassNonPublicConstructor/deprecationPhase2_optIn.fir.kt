// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@ConsistentCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.<!INVISIBLE_REFERENCE!>copy<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
