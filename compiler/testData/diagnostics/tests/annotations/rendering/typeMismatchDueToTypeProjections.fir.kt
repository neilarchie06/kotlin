// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
class C<T> {
    @A
    operator fun plus(x: Out<@A T>): @A C<@A T> = this
}

class Out<out F>

fun test(a: C<out CharSequence>, y: Out<CharSequence>) {
    a + <!ARGUMENT_TYPE_MISMATCH("Out<CharSequence>; Out<CapturedType(out CharSequence)>")!>y<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, capturedType, classDeclaration, functionDeclaration,
nullableType, operator, out, outProjection, thisExpression, typeParameter */
