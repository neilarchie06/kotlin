// RUN_PIPELINE_TILL: FRONTEND
package foo

import kotlin.reflect.KProperty

open class A {
    val B.w: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> <!CANNOT_INFER_PARAMETER_TYPE!>MyProperty<!>()
}

val B.r: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> <!CANNOT_INFER_PARAMETER_TYPE!>MyProperty<!>()

val A.e: Int by MyProperty()

class B {
    val A.f: Int by MyProperty()
}

class MyProperty<R : A, T> {
    operator fun getValue(thisRef: R, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, operator, propertyDeclaration,
propertyDelegate, propertyWithExtensionReceiver, starProjection, stringLiteral, typeConstraint, typeParameter */
