// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

@Serializable
data class WithTransients(<!TRANSIENT_MISSING_INITIALIZER!>@Transient<!> val missing: Int) {
    <!TRANSIENT_MISSING_INITIALIZER!>@Transient<!> val redundant: Int get() = 42 // incorrect in FIR

    @Transient
    lateinit var allowTransientLateinitWithoutInitializer: String
}
