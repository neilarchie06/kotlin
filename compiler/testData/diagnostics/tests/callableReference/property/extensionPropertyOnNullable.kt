// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

val Any?.meaning: Int
    get() = 42

fun test() {
    val f = Any?::meaning
    checkSubtype<Int>(f.get(null))
    checkSubtype<Int>(f.get(""))
}
