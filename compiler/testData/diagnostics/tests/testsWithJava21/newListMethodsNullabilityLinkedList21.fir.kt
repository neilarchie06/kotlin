// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_VERSION: 2.1
// API_VERSION: 2.1
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// DIAGNOSTICS: -JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE accidentally reported (K1 only) on x.addFirst/addLast/removeFirst/removeLast
// ISSUE: KT-68193
// ISSUE: KT-67804
import java.util.LinkedList

fun <E1> addFirstLast(sl: LinkedList<String>, el: LinkedList<E1>, ev: E1) {

    sl.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    sl.addFirst("")
    sl.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    sl.addLast("")

    el.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    el.addFirst(ev)
    el.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    el.addLast(ev)
}

fun removeFirstLastLinkedListString(s: LinkedList<String>) {
    var x1 = s.removeFirst()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

fun <E> removeFirstLastArrayListE(s: LinkedList<E>) {
    var x1 = s.removeFirst()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, nullableType, propertyDeclaration, stringLiteral,
typeParameter */
