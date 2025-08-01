// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmOverloads
fun foo(o: StringWrapper = StringWrapper("O"), k: String = "K"): String = ""
