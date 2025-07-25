// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class File(name: String)
interface InputStream

interface AutoCloseScope {
    fun defer(closeBlock: () -> Unit)
    fun close()
}
class AutoCloseScopeImpl : AutoCloseScope {
    override fun defer(closeBlock: () -> Unit) = TODO()
    override fun close() = TODO()
}

context(AutoCloseScope)
fun File.open(): InputStream = TODO()

fun withAutoClose(block: context(AutoCloseScope) () -> Unit) {
    val scope = AutoCloseScopeImpl() // Not shown here
    try {
        with(scope) { block() }
    } finally {
        scope.close()
    }
}

fun test() {
    withAutoClose {
        val input = File("input.txt").open()
        val config = File("config.txt").open()
        // Work
        // All files are closed at the end
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
functionalType, interfaceDeclaration, lambdaLiteral, localProperty, override, primaryConstructor, propertyDeclaration,
stringLiteral, tryExpression, typeWithContext */
