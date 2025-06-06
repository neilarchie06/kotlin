// JVM_DEFAULT_MODE: no-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Z<T> {
    fun test(p: T): T {
        return p
    }
}

open class ZImpl : Z<String>

class ZImpl2 : ZImpl() {

    override fun test(p: String): String {
        return super.test(p)
    }
}

fun box(): String {
    return ZImpl2().test("OK")
}
