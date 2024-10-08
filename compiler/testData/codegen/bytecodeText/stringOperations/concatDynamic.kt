// JVM_TARGET: 11
class A

inline class IC(val x: String)

inline fun test(s: (String) -> Unit) {
    s("456")
}

fun box(a: String, b: String?, x: IC?) {
    val p = 3147483648u
    val s = a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7' + A() + true + false + 3147483647u + p + x

    a.plus(b)
    b?.plus(a)
    val ref1 = a::plus
    val ref2 = b::plus

    test("123"::plus)
}

// 7 INVOKEDYNAMIC makeConcatWithConstants
// 1 "IC\(x=\\u0001\)"
// 0 append
// 0 stringPlus

//  unsigned constant 3147483647u is inlined
// 1 "\\u00011234\\u00015.06.07\\u0001truefalse3147483647\\u0001\\u0001"
// 1 INVOKESTATIC kotlin/UInt.toString-impl

// .. but ir backend performs wise `toString-impl` call
// one in IC.toString() + one in concatenation
// 2 INVOKESTATIC IC.toString-impl
