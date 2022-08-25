// This test checks that we don't generate target TYPE_USE if there's JDK 1.6 in the classpath.
// It's important that this test depends on _mock JDK_, which doesn't have ElementType.TYPE_USE.

// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class A

fun box(): String {
    val targets = A::class.java.getAnnotation(java.lang.annotation.Target::class.java).value
    if (targets.size != 0) return "Fail: Java annotation target list should be empty: ${targets.toList()}"

    return "OK"
}
