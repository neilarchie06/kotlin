// RUN_PIPELINE_TILL: FRONTEND
import java.io.Serializable

interface Test1 {
    <!CONFLICTING_OVERLOADS!>fun <T> foo(t: T)<!> where T : Cloneable, T : Serializable
    @Deprecated("foo", level = DeprecationLevel.HIDDEN)
    <!CONFLICTING_OVERLOADS!>fun <T> foo(t: T)<!> where T : Serializable, T : Cloneable
}


interface I1
interface I2 : I1

interface Test2 {
    <!CONFLICTING_OVERLOADS!>fun <T> foo(t: T)<!> where T : I1, T : I2
    @Deprecated("foo", level = DeprecationLevel.HIDDEN)
    <!CONFLICTING_OVERLOADS!>fun <T> foo(t: T)<!> where T : I2, T : I1
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, stringLiteral, typeConstraint, typeParameter */
