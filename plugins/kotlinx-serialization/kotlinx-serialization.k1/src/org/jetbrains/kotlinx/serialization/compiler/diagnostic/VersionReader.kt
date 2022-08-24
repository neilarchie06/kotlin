/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.openapi.util.io.JarUtil
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.getClassFromSerializationPackage
import java.io.File
import java.util.jar.Attributes

object VersionReader {

    fun getVersionsFromManifest(runtimeLibraryPath: File): RuntimeVersions {
        val version = JarUtil.getJarAttribute(runtimeLibraryPath, Attributes.Name.IMPLEMENTATION_VERSION)?.let(ApiVersion.Companion::parse)
        val kotlinVersion = JarUtil.getJarAttribute(runtimeLibraryPath, REQUIRE_KOTLIN_VERSION)?.let(ApiVersion.Companion::parse)
        return RuntimeVersions(version, kotlinVersion)
    }

    private val REQUIRE_KOTLIN_VERSION = Attributes.Name("Require-Kotlin-Version")
    private const val CLASS_SUFFIX = "!/kotlinx/serialization/KSerializer.class"

    private val VERSIONS_SLICE: WritableSlice<ModuleDescriptor, RuntimeVersions> = Slices.createSimpleSlice()

    fun getVersionsForCurrentModuleFromTrace(module: ModuleDescriptor, trace: BindingTrace): RuntimeVersions? {
        trace.get(VERSIONS_SLICE, module)?.let { return it }
        val versions = getVersionsForCurrentModule(module) ?: return null
        trace.record(VERSIONS_SLICE, module, versions)
        return versions
    }

    fun getVersionsForCurrentModuleFromContext(module: ModuleDescriptor, context: BindingContext?): RuntimeVersions? {
        context?.get(VERSIONS_SLICE, module)?.let { return it }
        return getVersionsForCurrentModule(module)
    }

    fun getVersionsForCurrentModule(module: ModuleDescriptor): RuntimeVersions? {
        val markerClass = module.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS)
        val location = (markerClass.source as? KotlinJvmBinarySourceElement)?.binaryClass?.location ?: return null
        val jarFile = location.removeSuffix(CLASS_SUFFIX)
        if (!jarFile.endsWith(".jar")) return null
        val file = File(jarFile)
        if (!file.exists()) return null
        return getVersionsFromManifest(file)
    }

    fun canSupportInlineClasses(module: ModuleDescriptor, trace: BindingTrace): Boolean {
        // Klibs do not have manifest file, unfortunately, so we hope for the better
        val currentVersion = getVersionsForCurrentModuleFromTrace(module, trace) ?: return true
        val implVersion = currentVersion.implementationVersion ?: return false
        return implVersion >= RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES
    }
}
