// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_TXT
@kotlin.coroutines.RestrictsSuspension
class RestrictedController {
    suspend fun member() {
        ext()
        member()
        memberExt()
    }

    suspend fun RestrictedController.memberExt() {
        ext()
        member()
        memberExt()
    }
}

suspend fun RestrictedController.ext() {
    ext()
    member()
    memberExt()
}

fun generate(c: suspend RestrictedController.() -> Unit) {}

fun runBlocking(x: suspend () -> Unit) {}

fun test() {
    generate a@{
        ext()
        member()
        memberExt()

        this@a.ext()
        this@a.member()
        this@a.memberExt()

        generate b@{
            ext()
            member()
            memberExt()

            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>ext<!>()
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memberExt<!>()

            this@b.ext()
            this@b.member()
            this@b.memberExt()
        }

        runBlocking {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>ext<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memberExt<!>()

            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>ext<!>()
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memberExt<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
suspend, thisExpression, typeWithExtension */
