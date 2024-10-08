// KJS_WITH_FULL_RUNTIME
package foo

class A {
    override fun hashCode() = 23456
}

fun box(): String {
    val x = A()
    val y = A()

    val map = mutableMapOf<A, Int>()
    map[x] = 1
    assertEquals(1, map.size)

    map.remove(y)
    assertEquals(1, map.size)

    map.remove(x)
    assertEquals(0, map.size)

    return "OK"
}