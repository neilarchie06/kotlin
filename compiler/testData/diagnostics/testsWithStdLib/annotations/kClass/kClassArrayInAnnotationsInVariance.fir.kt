// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KClass

open class A
class B1 : A()
class B2 : A()

annotation class Ann1(val arg: Array<KClass<in A>>)

@Ann1(arrayOf(A::class))
class MyClass1

@Ann1(arrayOf(Any::class))
class MyClass1a

@Ann1(<!ARGUMENT_TYPE_MISMATCH!>arrayOf(B1::class)<!>)
class MyClass2

annotation class Ann2(val arg: Array<KClass<in B1>>)

@Ann2(arrayOf(A::class))
class MyClass3

@Ann2(arrayOf(B1::class))
class MyClass4

@Ann2(<!ARGUMENT_TYPE_MISMATCH!>arrayOf(B2::class)<!>)
class MyClass5

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, collectionLiteral, inProjection,
primaryConstructor, propertyDeclaration */
