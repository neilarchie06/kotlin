import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(libs.intellij.asm)
    compileOnly(intellijPlatformUtil())
}

sourceSets {
    "main" { projectDefault() }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    @Suppress("DEPRECATION")
    compilerOptions.apiVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    @Suppress("DEPRECATION")
    compilerOptions.languageVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}
