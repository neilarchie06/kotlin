package foo

// CHECK_CONTAINS_NO_CALLS: test

internal class A(val x: Int) {
    inline fun f(): Int = x

    inline fun ff(): Int = f()
}

internal fun test(a: A): Int = a.ff()

fun box(): String {
    assertEquals(1, test(A(1)))
    assertEquals(2, test(A(2)))

    return "OK"
}