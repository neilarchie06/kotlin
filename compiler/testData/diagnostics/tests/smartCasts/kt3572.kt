// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Printer {
    fun print()
}

class OKPrinter : Printer {
    override fun print() {  }
}

class MyClass(var printer: Printer)


fun main(m: MyClass) {
    if (m.printer is OKPrinter) {
        // We do not need smart cast here, so we should not get SMARTCAST_IMPOSSIBLE
        m.printer.print()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, isExpression, override,
primaryConstructor, propertyDeclaration, smartcast */
