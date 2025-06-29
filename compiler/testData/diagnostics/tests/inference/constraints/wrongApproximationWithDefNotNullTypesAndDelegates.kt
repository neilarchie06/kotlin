// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class Foo {
    var test: String by refreshOnUpdate("str")

    fun <T> refreshOnUpdate(initialValue: T) = RefreshDelegate(initialValue)

    class RefreshDelegate<T>(initialValue: T?) {
        operator fun getValue(thisRef: Foo, property: KProperty<*>): T = TODO()

        operator fun setValue(thisRef: Foo, property: KProperty<*>, value: T) {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, nullableType, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, setter, starProjection, stringLiteral, typeParameter */
