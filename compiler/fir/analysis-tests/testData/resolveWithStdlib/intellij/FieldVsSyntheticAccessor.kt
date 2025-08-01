// RUN_PIPELINE_TILL: FRONTEND
// FILE: BaseClass.java

public class BaseClass {
    protected String ui = "";
}

// FILE: User.kt
package test

import BaseClass

class User : BaseClass() {
    fun foo(tree: BaseClass) {
        val ui = tree.<!INVISIBLE_REFERENCE!>ui<!>
    }

    fun bar() {
        val ui = ui
    }

    fun baz() {
        val ui = this.ui
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaProperty, javaType, localProperty,
propertyDeclaration, thisExpression */
