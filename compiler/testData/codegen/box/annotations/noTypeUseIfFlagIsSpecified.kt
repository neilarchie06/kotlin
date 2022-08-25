// This test checks that we don't generate target TYPE_USE if `-Xno-new-java-annotation-targets` is used.
// It's important that this test depends on _full JDK_, which has ElementType.TYPE_USE, to check that filtering based on
// the compiler argument is taking place.

// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// NO_NEW_JAVA_ANNOTATION_TARGETS
// FULL_JDK
// WITH_STDLIB

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class A

fun box(): String {
    val targets = A::class.java.getAnnotation(java.lang.annotation.Target::class.java).value
    if (targets.size != 0) return "Fail: Java annotation target list should be empty: ${targets.toList()}"

    return "OK"
}
