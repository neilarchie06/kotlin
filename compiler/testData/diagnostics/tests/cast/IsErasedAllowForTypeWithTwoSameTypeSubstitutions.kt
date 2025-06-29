// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class BaseMulti<out A, B>
class SomeMultiDerived<out D>: BaseMulti<D, Any>()

// t is BaseMulti<+String, String> => if (t is SomeMultiDerived<?>) => t is SomeMultiDerived<+String> =>
//     => (String <: Any, SomeMultiDerived<Covariant D>) t is SomeMultiDerived<+Any>
fun someDerived(t: BaseMulti<String, String>) = t is SomeMultiDerived<Any>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, nullableType, out, typeParameter */
