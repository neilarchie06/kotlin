// RUN_PIPELINE_TILL: FRONTEND
// ### INVALID

// FILE: invalid_noargs.kt
package invalid1
<!CONFLICTING_OVERLOADS!>fun main()<!> {}
<!CONFLICTING_OVERLOADS!>fun main()<!> {}

// FILE: invalid_array.kt
package invalid2
<!CONFLICTING_OVERLOADS!>fun main(args: Array<String>)<!> {}
<!CONFLICTING_OVERLOADS!>fun main(args: Array<String>)<!> {}

// FILE: invalid_vararg.kt
package invalid3
<!CONFLICTING_OVERLOADS!>fun main(vararg args: String)<!> {}
<!CONFLICTING_OVERLOADS!>fun main(vararg args: String)<!> {}

// FILE: invalid_array_typealias.kt
package invalid4
typealias S = String
<!CONFLICTING_OVERLOADS!>fun main(args: Array<String>)<!> {}
<!CONFLICTING_OVERLOADS!>fun main(args: Array<S>)<!> {}

// FILE: invalid_noargs_suspend.kt
package invalid5
suspend <!CONFLICTING_OVERLOADS!>fun main()<!> {}
suspend <!CONFLICTING_OVERLOADS!>fun main()<!> {}

// FILE: invalid_array_suspend.kt
package invalid6
suspend <!CONFLICTING_OVERLOADS!>fun main(args: Array<String>)<!> {}
suspend <!CONFLICTING_OVERLOADS!>fun main(args: Array<String>)<!> {}

// FILE: invalid_vararg_suspend.kt
package invalid7
suspend <!CONFLICTING_OVERLOADS!>fun main(vararg args: String)<!> {}
suspend <!CONFLICTING_OVERLOADS!>fun main(vararg args: String)<!> {}

// FILE: invalid_array_typealias_suspend.kt
package invalid8
typealias S = String
suspend <!CONFLICTING_OVERLOADS!>fun main(args: Array<String>)<!> {}
suspend <!CONFLICTING_OVERLOADS!>fun main(args: Array<S>)<!> {}

// ### VALID

// FILE: valid_noargs.kt
package valid1
fun main() {}

// FILE: valid_noargs2.kt
package valid1
fun main() {}

// FILE: valid_array.kt
package valid2
fun main(args: Array<String>) {}

// FILE: valid_array2.kt
package valid2
fun main(args: Array<String>) {}

// FILE: valid_vararg.kt
package valid3
fun main(vararg args: String) {}

// FILE: valid_vararg2.kt
package valid3
fun main(vararg args: String) {}

// FILE: valid_array_typealias.kt
package valid4
fun main(args: Array<String>) {}

// FILE: valid_array_typealias2.kt
package valid4
typealias S = String
fun main(args: Array<S>) {}

// FILE: valid_noargs_suspend.kt
package valid5
suspend fun main() {}

// FILE: valid_noargs_suspend2.kt
package valid5
suspend fun main() {}

// FILE: valid_array_suspend.kt
package valid6
suspend fun main(args: Array<String>) {}

// FILE: valid_array_suspend2.kt
package valid6
suspend fun main(args: Array<String>) {}

// FILE: valid_vararg_suspend.kt
package valid7
suspend fun main(vararg args: String) {}

// FILE: valid_vararg_suspend2.kt
package valid7
suspend fun main(vararg args: String) {}

// FILE: valid_array_typealias_suspend.kt
package valid8
suspend fun main(args: Array<String>) {}

// FILE: valid_array_typealias_suspend2.kt
package valid8
typealias S = String
suspend fun main(args: Array<S>) {}

/* GENERATED_FIR_TAGS: functionDeclaration, suspend, typeAliasDeclaration, vararg */
