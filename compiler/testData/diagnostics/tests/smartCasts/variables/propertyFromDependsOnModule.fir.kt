// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT

// MODULE: transitive
// FILE: Transitive.kt
annotation class Ann

class Transitive(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}


// MODULE: direct()()(transitive)
// FILE: Direct.kt
class Direct(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}

// MODULE: app()()(direct)
class Same(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}

fun isCast(s: Same, d: Direct, t: Transitive) {
    if (s.finalConstructorProperty is String) {
        s.finalConstructorProperty.length
    }

    if (d.finalConstructorProperty is String) {
        d.finalConstructorProperty.length
    }

    if (t.finalConstructorProperty is String) {
        t.finalConstructorProperty.length
    }

    if (s.annotatedConstructorProperty is String) {
        s.annotatedConstructorProperty.length
    }

    if (d.annotatedConstructorProperty is String) {
        d.annotatedConstructorProperty.length
    }

    if (t.annotatedConstructorProperty is String) {
        t.annotatedConstructorProperty.length
    }

    if (s.finalClassProperty is String) {
        s.finalClassProperty.length
    }

    if (d.finalClassProperty is String) {
        d.finalClassProperty.length
    }

    if (t.finalClassProperty is String) {
        t.finalClassProperty.length
    }

    if (s.annotatedClassProperty is String) {
        s.annotatedClassProperty.length
    }

    if (d.annotatedClassProperty is String) {
        d.annotatedClassProperty.length
    }

    if (t.annotatedClassProperty is String) {
        t.annotatedClassProperty.length
    }
}

fun asCast(s: Same, d: Direct, t: Transitive) {
    s.finalConstructorProperty as String
    s.finalConstructorProperty.length

    d.finalConstructorProperty as String
    d.finalConstructorProperty.length

    t.finalConstructorProperty as String
    t.finalConstructorProperty.length

    s.annotatedConstructorProperty as String
    s.annotatedConstructorProperty.length

    d.annotatedConstructorProperty as String
    d.annotatedConstructorProperty.length

    t.annotatedConstructorProperty as String
    t.annotatedConstructorProperty.length

    s.finalClassProperty as String
    s.finalClassProperty.length

    d.finalClassProperty as String
    d.finalClassProperty.length

    t.finalClassProperty as String
    t.finalClassProperty.length

    s.annotatedClassProperty as String
    s.annotatedClassProperty.length

    d.annotatedClassProperty as String
    d.annotatedClassProperty.length

    t.annotatedClassProperty as String
    t.annotatedClassProperty.length
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetPropertyGetter, asExpression, classDeclaration,
functionDeclaration, ifExpression, isExpression, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
