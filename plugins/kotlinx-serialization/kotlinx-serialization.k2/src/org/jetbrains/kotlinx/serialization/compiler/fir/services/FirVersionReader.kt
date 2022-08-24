/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.services

import com.intellij.openapi.util.io.JarUtil
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.RuntimeVersions
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialClassIds
import java.io.File
import java.util.jar.Attributes

class FirVersionReader(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        private val REQUIRE_KOTLIN_VERSION = Attributes.Name("Require-Kotlin-Version")
        private const val CLASS_SUFFIX = "!/kotlinx/serialization/KSerializer.class"
    }

    val runtimeVersions: RuntimeVersions? by session.firCachesFactory.createLazyValue lazy@{
        val markerClass = session.symbolProvider.getClassLikeSymbolByClassId(SerialClassIds.KSERIALIZER_CLASS_ID) ?: return@lazy null
        val location = (markerClass.sourceElement as? KotlinJvmBinarySourceElement)?.binaryClass?.location ?: return@lazy null
        val jarFile = location.removeSuffix(CLASS_SUFFIX)
        if (!jarFile.endsWith(".jar")) return@lazy null
        val file = File(jarFile)
        if (!file.exists()) return@lazy null
        getVersionsFromManifest(file)
    }

    val canSupportInlineClasses by session.firCachesFactory.createLazyValue lazy@{
        val currentVersion = runtimeVersions ?: return@lazy true
        val implVersion = currentVersion.implementationVersion ?: return@lazy false
        implVersion >= RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES
    }

    private fun getVersionsFromManifest(runtimeLibraryPath: File): RuntimeVersions {
        val version = JarUtil.getJarAttribute(runtimeLibraryPath, Attributes.Name.IMPLEMENTATION_VERSION)?.let(ApiVersion.Companion::parse)
        val kotlinVersion = JarUtil.getJarAttribute(runtimeLibraryPath, REQUIRE_KOTLIN_VERSION)?.let(ApiVersion.Companion::parse)
        return RuntimeVersions(version, kotlinVersion)
    }
}

val FirSession.versionReader: FirVersionReader by FirSession.sessionComponentAccessor()

