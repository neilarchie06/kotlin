// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class Ann
annotation class Ann2
interface I {}
class C {
    fun foo() {
        class Local {
            @<!UNRESOLVED_REFERENCE!>Ann0<!>
            @Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
            @Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
            @<!UNRESOLVED_REFERENCE!>Ann4<!><!SYNTAX!><!>
        }
        val i = object : I {
            @<!UNRESOLVED_REFERENCE!>Ann0<!>
            @Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
            @Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
            @<!UNRESOLVED_REFERENCE!>Ann4<!><!SYNTAX!><!>
        }
    }
    @<!UNRESOLVED_REFERENCE!>Ann0<!>
    @Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
    @Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
    @<!UNRESOLVED_REFERENCE!>Ann4<!><!SYNTAX!><!>
}

@<!UNRESOLVED_REFERENCE!>Ann0<!>
@Ann @<!UNRESOLVED_REFERENCE!>Ann3<!>
@Ann2(<!TOO_MANY_ARGUMENTS!>1<!>)
@<!UNRESOLVED_REFERENCE!>Ann4<!><!SYNTAX!><!>

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, functionDeclaration,
integerLiteral, interfaceDeclaration, localClass, localProperty, propertyDeclaration */
