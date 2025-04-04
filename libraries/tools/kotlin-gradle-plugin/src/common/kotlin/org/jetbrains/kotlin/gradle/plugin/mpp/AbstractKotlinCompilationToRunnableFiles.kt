/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinAnyOptionsDeprecated
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

@Deprecated("Use KotlinCompilation<T> instead. Scheduled for removal in Kotlin 2.3.", level = DeprecationLevel.ERROR)
abstract class AbstractKotlinCompilationToRunnableFiles<T : KotlinAnyOptionsDeprecated>
internal constructor(compilation: KotlinCompilationImpl) : AbstractKotlinCompilation<T>(compilation), KotlinCompilationToRunnableFiles<T> {

    final override val runtimeDependencyConfigurationName: String
        get() = compilation.runtimeDependencyConfigurationName ?: error("$compilation: Missing 'runtimeDependencyConfigurationName'")

    final override var runtimeDependencyFiles: FileCollection
        get() = compilation.runtimeDependencyFiles ?: error("$compilation: Missing 'runtimeDependencyFiles'")
        set(value) {
            compilation.runtimeDependencyFiles = value
        }
}

internal typealias DeprecatedAbstractKotlinCompilationToRunnableFiles<T> = AbstractKotlinCompilationToRunnableFiles<T>
