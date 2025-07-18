// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_PARAMETER, -INLINE_CLASS_DEPRECATED

inline class A0(val x: Int)

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>inline<!> class A1
inline class A2<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>
inline class A3(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
inline class A4(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>)
inline class A5<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>
inline class A6<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(x: Int, val y: Int)<!>
inline class A7(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg val x: Int<!>)
inline class A8(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>)
inline class A9(final val x: Int)

class B1 {
    companion object {
        inline class C1(val x: Int)
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> <!VALUE_CLASS_NOT_TOP_LEVEL!>inline<!> class C11(val x: Int)
    }

    inline class C2(val x: Int)
    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>inline<!> class C21(val x: Int)
}

object B2 {
    inline class C3(val x: Int)
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> <!VALUE_CLASS_NOT_TOP_LEVEL!>inline<!> class C31(val x: Int)
}

fun foo() {
    <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>inline<!> class C4(val x: Int)
}

final inline class D0(val x: Int)
<!VALUE_CLASS_NOT_FINAL!>open<!> inline class D1(val x: Int)
<!VALUE_CLASS_NOT_FINAL!>abstract<!> inline class D2(val x: Int)
<!VALUE_CLASS_NOT_FINAL!>sealed<!> inline class D3(val x: Int)

<!INCOMPATIBLE_MODIFIERS!>inline<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class D4(val x: String)

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, functionDeclaration, inner, localClass, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration, sealed, vararg */
