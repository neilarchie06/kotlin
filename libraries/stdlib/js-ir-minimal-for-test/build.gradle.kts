import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
    id("nodejs-cache-redirector-configuration")
}

kotlin {
    js(IR) {
        nodejs()
    }
}
val commonMainFullSources by task<Sync> {
    dependsOn(":prepare:build.version:writeStdlibVersion")

    val sources = listOf(
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/",
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }

    into(layout.buildDirectory.dir("commonMainFullSources"))
}

val commonNonJvmMainFullSources by task<Sync> {
    val sources = listOf(
        "libraries/stdlib/common-non-jvm/src/",
    )
    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }
    into(layout.buildDirectory.dir("commonNonJvmMainFullSources"))
}

val commonMainSources by task<Sync> {
    dependsOn(commonMainFullSources)
    from {
        exclude(
            listOf(
                "libraries/stdlib/unsigned/src/kotlin/UByteArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/UIntArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/ULongArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/UMath.kt",
                "libraries/stdlib/unsigned/src/kotlin/UNumbers.kt",
                "libraries/stdlib/unsigned/src/kotlin/UShortArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/UStrings.kt",
                "libraries/stdlib/common/src/generated/_Arrays.kt",
                "libraries/stdlib/common/src/generated/_Collections.kt",
                "libraries/stdlib/common/src/generated/_Comparisons.kt",
                "libraries/stdlib/common/src/generated/_Maps.kt",
                "libraries/stdlib/common/src/generated/_OneToManyTitlecaseMappings.kt",
                "libraries/stdlib/common/src/generated/_Sequences.kt",
                "libraries/stdlib/common/src/generated/_Sets.kt",
                "libraries/stdlib/common/src/generated/_Strings.kt",
                "libraries/stdlib/common/src/generated/_UArrays.kt",
                "libraries/stdlib/common/src/generated/_URanges.kt",
                "libraries/stdlib/common/src/generated/_UCollections.kt",
                "libraries/stdlib/common/src/generated/_UComparisons.kt",
                "libraries/stdlib/common/src/generated/_USequences.kt",
                "libraries/stdlib/common/src/kotlin/SequencesH.kt",
                "libraries/stdlib/common/src/kotlin/TextH.kt",
                "libraries/stdlib/common/src/kotlin/UMath.kt",
                "libraries/stdlib/common/src/kotlin/collections/**",
                "libraries/stdlib/common/src/kotlin/ioH.kt",
                "libraries/stdlib/src/kotlin/collections/**",
                "libraries/stdlib/src/kotlin/io/**",
                "libraries/stdlib/src/kotlin/properties/Delegates.kt",
                "libraries/stdlib/src/kotlin/random/URandom.kt",
                "libraries/stdlib/src/kotlin/text/**",
                "libraries/stdlib/src/kotlin/time/**",
                "libraries/stdlib/src/kotlin/util/KotlinVersion.kt",
                "libraries/stdlib/src/kotlin/util/Tuples.kt",
                "libraries/stdlib/src/kotlin/uuid/**",
                "libraries/stdlib/src/kotlin/enums/**"
            )
        )
        commonMainFullSources.get().outputs.files.singleFile
    }

    into(layout.buildDirectory.dir("commonMainSources"))
}

val commonMainCollectionSources by task<Sync> {
    dependsOn(commonMainFullSources)
    from {
        include("libraries/stdlib/src/kotlin/collections/PrimitiveIterators.kt")
        commonMainFullSources.get().outputs.files.singleFile
    }

    into(layout.buildDirectory.dir("commonMainCollectionSources"))
}

val commonNonJvmMainSources by task<Sync> {
    dependsOn(commonNonJvmMainFullSources)
    from {
        commonNonJvmMainFullSources.get().outputs.files.singleFile
    }

    into(layout.buildDirectory.dir("commonNonJvmMainSources"))
}

val commonJsAndWasmJsSources by task<Sync> {
    val jsAndWasmJsDir = file("$rootDir/libraries/stdlib/common-js-wasmjs")

    from("$jsAndWasmJsDir/src") {
        include(
            "kotlin/js/annotations.kt",
            "kotlin/js/ExperimentalWasmJsInterop.kt",
            "kotlin/js/core.kt",
        )
    }

    into(layout.buildDirectory.dir("commonJsAndWasmJsSources"))
}

val jsMainSources by task<Sync> {
    dependsOn(":kotlin-stdlib:prepareJsIrMainSources")
    val jsDir = file("$rootDir/libraries/stdlib/js")

    from("$jsDir/src") {
        exclude(
            "generated/**",
            "org.w3c/**",
            "kotlin/char.kt",
            "kotlin/collectionJs.kt",
            "kotlin/js.collections.kt",
            "kotlin/collections/**",
            "kotlin/time/**",
            "kotlin/console.kt",
            "kotlin/coreDeprecated.kt",
            "kotlin/date.kt",
            "kotlin/GroupingJs.kt",
            "kotlin/ItemArrayLike.kt",
            "kotlin/io/**",
            "kotlin/wasmJs/**",
            "kotlin/json.kt",
            "kotlin/Promise.kt",
            "kotlin/regexp.kt",
            "kotlin/sequenceJs.kt",
            "kotlin/throwableExtensions.kt",
            "kotlin/text/**",
            "kotlin/reflect/KTypeHelpers.kt",
            "kotlin/reflect/KTypeHelpers.old.kt",
            "kotlin/reflect/KTypeImpl.kt",
            "kotlin/dom/**",
            "kotlin/browser/**",
            "kotlinx/dom/**",
            "kotlinx/browser/**",
            "kotlin/enums/**",
            "kotlin/uuid/UuidJs.kt",
        )
    }
    from {
        val fullJsMainSources = tasks.getByPath(":kotlin-stdlib:prepareJsIrMainSources") as Sync
        fullJsMainSources.destinationDir
    }
    from("$jsDir/runtime") {
        exclude("collectionsHacks.kt")
        exclude("collectionsInterop.kt")
        into("runtime")
    }
    from("$jsDir/builtins") {
        exclude("Collections.kt")
        into("builtins")
    }

    into(layout.buildDirectory.dir("jsMainSources"))
}

kotlin {
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(files(commonMainSources.map { it.destinationDir }))
            kotlin.srcDir(files(commonMainCollectionSources.map { it.destinationDir }))
            kotlin.srcDir("common-src")
        }
        val commonNonJvmMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir(files(commonNonJvmMainSources.map { it.destinationDir }))
        }
        val commonJsAndWasmJs by creating {
            dependsOn(commonMain)
            kotlin.srcDir(files(commonJsAndWasmJsSources.map { it.destinationDir }))
        }
        named("jsMain") {
            dependsOn(commonJsAndWasmJs)
            dependsOn(commonNonJvmMain)
            kotlin.srcDir(files(jsMainSources.map { it.destinationDir }))
            kotlin.srcDir("js-src")
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        compilerOptions.languageVersion = KotlinVersion.KOTLIN_2_3
        compilerOptions.apiVersion = KotlinVersion.KOTLIN_2_3
        compilerOptions.freeCompilerArgs.addAll(
            listOf(
                "-Xallow-kotlin-package",
                "-Xexpect-actual-classes",
                "-Xstdlib-compilation",
                "-Xdont-warn-on-error-suppression",
                "-opt-in=kotlin.ExperimentalMultiplatform",
                "-opt-in=kotlin.contracts.ExperimentalContracts",
                "-Xcontext-parameters",
                // See allowReturnValueCheckerButNotReport() in libraries/stdlib/build.gradle.kts:
                "-Xreturn-value-checker=check",
                "-Xwarning-level=RETURN_VALUE_NOT_USED:disabled",
            )
        )
    }
}

tasks {
    compileKotlinMetadata {
        enabled = false
    }

    named<KotlinCompilationTask<*>>("compileKotlinJs") {
        compilerOptions.freeCompilerArgs.add("-Xir-module-name=kotlin")
    }
}
