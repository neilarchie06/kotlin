// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64840

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface CommonSupertype
interface A<F> : CommonSupertype
interface B : A<String>
interface C : CommonSupertype

fun <X> predicate(x: X, c: Controller<X>, p: (X) -> Boolean) {}

fun main(a: A<String>, c: C) {
    val x2 = generate {
        predicate(a, this) { x ->
            // USELESS_IS_CHECK is errorenously reported in K1
            <!USELESS_IS_CHECK!>x is B<!>
        }

        yield(c)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("CommonSupertype")!>x2<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, interfaceDeclaration, isExpression,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, suspend, thisExpression, typeParameter,
typeWithExtension */
