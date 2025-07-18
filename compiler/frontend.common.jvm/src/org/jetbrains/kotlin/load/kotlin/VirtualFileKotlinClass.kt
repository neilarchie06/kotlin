/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseSideType
import org.jetbrains.kotlin.util.tryMeasureSideTime
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

interface LibraryContainerAwareVirtualFile {
    fun getContainingLibraryPath(): Path
}

class VirtualFileKotlinClass private constructor(
    val file: VirtualFile,
    className: ClassId,
    classVersion: Int,
    classHeader: KotlinClassHeader,
    innerClasses: InnerClassesInfo
) : FileBasedKotlinClass(className, classVersion, classHeader, innerClasses) {

    override val location: String
        get() = file.path

    override val containingLibrary: String?
        get() = file.path.split("!/").firstOrNull()

    override val containingLibraryPath: PathHolder?
        // we should return not the file itself, but the root - LibraryPathFilter later uses `startsWith`
        get() {
            val containingLibraryPath =
                if (file is LibraryContainerAwareVirtualFile) file.getContainingLibraryPath() else containingLibrary?.let { Paths.get(it) }
            return containingLibraryPath?.let {
                PathHolder(containingLibraryPath)
            }
        }

    override fun getFileContents(): ByteArray {
        try {
            return file.contentsToByteArray()
        } catch (e: IOException) {
            throw logFileReadingErrorMessage(e, file)
        }
    }

    override fun equals(other: Any?) = other is VirtualFileKotlinClass && other.file == file
    override fun hashCode() = file.hashCode()
    override fun toString() = "${this::class.java.simpleName}: $file"

    companion object Factory {
        private val LOG = Logger.getInstance(VirtualFileKotlinClass::class.java)

        internal fun create(
            file: VirtualFile,
            metadataVersion: MetadataVersion,
            fileContent: ByteArray?,
            perfManager: PerformanceManager?,
        ): KotlinClassFinder.Result? {
            return perfManager.tryMeasureSideTime(PhaseSideType.BinaryClassFromKotlinFile) {
                assert(file.extension == JavaClassFileType.INSTANCE.defaultExtension || file.fileType == JavaClassFileType.INSTANCE) { "Trying to read binary data from a non-class file $file" }

                try {
                    val byteContent = fileContent ?: file.contentsToByteArray(false)
                    if (byteContent.isNotEmpty()) {
                        val kotlinJvmBinaryClass = create(byteContent, metadataVersion) { name, classVersion, header, innerClasses ->
                            VirtualFileKotlinClass(file, name, classVersion, header, innerClasses)
                        }

                        return@tryMeasureSideTime kotlinJvmBinaryClass?.let { KotlinClass(it, byteContent) }
                            ?: KotlinClassFinder.Result.ClassFileContent(byteContent)
                    }
                } catch (_: FileNotFoundException) {
                    // Valid situation. User can delete jar file.
                } catch (_: java.nio.file.NoSuchFileException) {
                    // Same (see FL-32618 as an exception example)
                } catch (e: Throwable) {
                    if (e is ControlFlowException) throw e
                    throw logFileReadingErrorMessage(e, file)
                }
                null
            }
        }

        private fun logFileReadingErrorMessage(e: Throwable, file: VirtualFile): Throwable {
            val errorMessage = renderFileReadingErrorMessage(file)
            LOG.warn(errorMessage, e)
            return IllegalStateException(errorMessage, e)
        }

        private fun renderFileReadingErrorMessage(file: VirtualFile): String =
            "Could not read file: ${file.path}; size in bytes: ${file.length}; file type: ${file.fileType.name}"
    }
}
