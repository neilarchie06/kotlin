// APPROXIMATE_TYPE
interface T
interface T2

fun foo() {
    open class A : T
    class B : A(), T2

    return <expr>B()</expr>
}

fun bar() {
    open class A : T
    class B : A(), T2

    object Some<caret>thing
}