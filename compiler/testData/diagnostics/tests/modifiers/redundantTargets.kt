// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface First
// Now inspection
abstract interface Second
// Now inspection
final enum class Third {
    FOURTH, 
    FIFTH
}
// Now inspection
final object Sixth

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, interfaceDeclaration, objectDeclaration */
