// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

class NonSerializable

@Serializable
class Basic(val foo: <!SERIALIZER_NOT_FOUND!>NonSerializable<!>)

@Serializable
class Inside(val foo: <!SERIALIZER_NOT_FOUND!>List<NonSerializable><!>)

@Serializable
class WithImplicitType {
    <!SERIALIZER_NOT_FOUND!>val foo = NonSerializable()<!>
}
