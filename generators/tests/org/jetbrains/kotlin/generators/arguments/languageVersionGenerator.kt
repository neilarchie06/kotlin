/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

internal fun generateLanguageVersion(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val languageVersionFqName = FqName("org.jetbrains.kotlin.gradle.dsl.LanguageVersion")
    filePrinter(file(apiDir, languageVersionFqName)) {
        generateDeclaration("enum class", languageVersionFqName, afterType = "(val version: String)") {
            val languageVersions = LanguageVersion.values()

            val lastIndex = languageVersions.size - 1
            languageVersions.forEachIndexed { index, languageVersion ->
                val lastChar = if (index == lastIndex) ";" else ","
                println("KOTLIN_${languageVersion.major}_${languageVersion.minor}(\"${languageVersion.versionString}\")$lastChar")
            }

            println()
            println("companion object {")
            withIndent {
                println("fun fromVersion(version: String): LanguageVersion =")
                println("    LanguageVersion.values().firstOrNull { it.version == version }")
                println("        ?: throw IllegalArgumentException(\"Unknown Kotlin language version: ${'$'}version\")")
            }
            println("}")
        }
    }
}