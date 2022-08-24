/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.RuntimeVersions
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.fir.serializerForClass
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.findTypeSerializerOrContextUnchecked
import org.jetbrains.kotlinx.serialization.compiler.fir.services.serializablePropertiesProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.versionReader

object FirSerializationPluginClassChecker : FirClassChecker() {
    private val JAVA_SERIALIZABLE_ID = ClassId.topLevel(FqName("java.io.Serializable"))

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            val classSymbol = declaration.symbol
            checkEnum(classSymbol, reporter)
            checkExternalSerializer(classSymbol, reporter)
            if (!canBeSerializedInternally(classSymbol, reporter)) return
            if (classSymbol !is FirRegularClassSymbol) return

            val properties = buildSerializableProperties(classSymbol, reporter) ?: return
            checkCorrectTransientAnnotationIsUsed(classSymbol, properties.serializableProperties, reporter)
            checkTransients(classSymbol, reporter)
            analyzePropertiesSerializers(classSymbol, properties.serializableProperties, reporter)
            // checkInheritedAnnotations(descriptor, declaration, context.trace)
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkExternalSerializer(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        val serializableKType = classSymbol.serializerForClass ?: return
        val serializableClassSymbol = serializableKType.toRegularClassSymbol(session) ?: return
        val properties = session.serializablePropertiesProvider.getSerializablePropertiesForClass(classSymbol)
        if (!properties.isExternallySerializable) {
            val source = classSymbol.serializerAnnotation?.source ?: classSymbol.source
            val error = if (serializableClassSymbol.moduleData == session.moduleData) {
                FirSerializationErrors.EXTERNAL_CLASS_NOT_SERIALIZABLE
            } else {
                FirSerializationErrors.EXTERNAL_CLASS_IN_ANOTHER_MODULE
            }
            reporter.reportOn(source, error, serializableClassSymbol, serializableKType)
        }

    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkInheritedAnnotations(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {

    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkMinRuntime(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {

    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkMinKotlin(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {

    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkCorrectTransientAnnotationIsUsed(
        classSymbol: FirClassSymbol<*>,
        properties: List<FirSerializableProperty>,
        reporter: DiagnosticReporter
    ) {
        if (classSymbol.superConeTypes.any { it.classId == JAVA_SERIALIZABLE_ID }) return // do not check
        for (property in properties) {
            if (property.transient) continue
            val incorrectTransient = property.propertySymbol.backingFieldSymbol?.annotations?.getAnnotationByClassId(TRANSIENT_ANNOTATION_CLASS_ID)
            if (incorrectTransient != null) {
                reporter.reportOn(
                    source = incorrectTransient.source ?: property.propertySymbol.source,
                    FirSerializationErrors.INCORRECT_TRANSIENT
                )
            }
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun canBeSerializedInternally(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter): Boolean {
        // if enum has meta or SerialInfo annotation on a class or entries and used plugin-generated serializer
        if (session.dependencySerializationInfoProvider.useGeneratedEnumSerializer && classSymbol.isSerializableEnumWithMissingSerializer) {
            reporter.reportOn(
                classSymbol.source,
                FirSerializationErrors.EXPLICIT_SERIALIZABLE_IS_REQUIRED,
                positioningStrategy = SourceElementPositioningStrategies.DEFAULT // TODO add modifiers positioning strategy
            )
            return false
        }
        
        if (!classSymbol.hasSerializableOrMetaAnnotation) return false
        
        if (classSymbol.isAnonymousObjectOrInsideIt) {
            reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.ANONYMOUS_OBJECTS_NOT_SUPPORTED)
            return false
        }

        if (classSymbol.isInner) {
            reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.INNER_CLASSES_NOT_SUPPORTED)
            return false
        }

        if (classSymbol.isInline && !session.versionReader.canSupportInlineClasses) {
            reporter.reportOn(
                classSymbol.serializableOrMetaAnnotationSource,
                FirSerializationErrors.INLINE_CLASSES_NOT_SUPPORTED,
                RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES.toString(),
                session.versionReader.runtimeVersions?.implementationVersion.toString()
            )
            return false
        }

        if (!classSymbol.hasSerializableOrMetaAnnotationWithoutArgs) {
            // defined custom serializer
            checkClassWithCustomSerializer(classSymbol, reporter)
            return false
        }

        if (classSymbol.serializableAnnotationIsUseless) {
            reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED)
            return false
        }

        // check that we can instantiate supertype
        if (!classSymbol.isEnumClass) { // enums are inherited from java.lang.Enum and can't be inherited from other classes
            val superClassSymbol = classSymbol.getSuperClassOrAny(session)
            if (!superClassSymbol.isInternalSerializable) {
                val noArgConstructorSymbol = superClassSymbol.declarationSymbols.firstOrNull { it is FirConstructorSymbol && it.valueParameterSymbols.isEmpty() }
                if (noArgConstructorSymbol == null) {
                    reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR)
                    return false
                }
            }
        }

        return true
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkClassWithCustomSerializer(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        val serializerType = classSymbol.serializableWith?.fullyExpandedType(session) ?: return
        checkCustomSerializerMatch(classSymbol, source = null, classSymbol.defaultType(), serializerType, reporter)
        checkCustomSerializerIsNotLocal(source = null, classSymbol, serializerType, reporter)
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private val FirClassSymbol<*>.isAnonymousObjectOrInsideIt: Boolean
        get() {
            if (this is FirAnonymousObjectSymbol) return true
            return containingDeclarations.any { it is FirAnonymousObject }
        }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkEnum(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        if (!classSymbol.isEnumClass) return
        val entryBySerialName = mutableMapOf<String, FirEnumEntrySymbol>()
        for (enumEntrySymbol in classSymbol.collectEnumEntries()) {
            val serialNameAnnotation = enumEntrySymbol.serialNameAnnotation
            val serialName = serialNameAnnotation?.getStringArgument(AnnotationParameterNames.VALUE) ?: enumEntrySymbol.name.asString()
            val firstEntry = entryBySerialName[serialName]
            if (firstEntry != null) {
                reporter.reportOn(
                    source = serialNameAnnotation?.source ?: firstEntry.serialNameAnnotation?.source ?: enumEntrySymbol.source,
                    FirSerializationErrors.DUPLICATE_SERIAL_NAME_ENUM,
                    classSymbol,
                    serialName,
                    enumEntrySymbol.name.asString()
                )
            } else {
                entryBySerialName[serialName] = enumEntrySymbol
            }
        }
    }


    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun buildSerializableProperties(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter): FirSerializableProperties? {
        if (!classSymbol.hasSerializableOrMetaAnnotation) return null
        if (!classSymbol.isInternalSerializable) return null
        if (classSymbol.hasCompanionObjectAsSerializer) return null

        val properties = session.serializablePropertiesProvider.getSerializablePropertiesForClass(classSymbol)
        if (!properties.isExternallySerializable) {
            reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY)
        }

        // check that all names are unique
        val namesSet = mutableSetOf<String>()
        for (property in properties.serializableProperties) {
            val name = property.name
            if (!namesSet.add(name)) {
                reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.DUPLICATE_SERIAL_NAME, name)
            }
        }
        return properties
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkTransients(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        for (propertySymbol in classSymbol.declarationSymbols.filterIsInstance<FirPropertySymbol>()) {
            val isInitialized = propertySymbol.isLateInit || declarationHasInitializer(propertySymbol)
            val transientAnnotation = propertySymbol.serialTransientAnnotation ?: continue
            val hasBackingField = propertySymbol.hasBackingField
            val source = transientAnnotation.source ?: propertySymbol.source
            if (!hasBackingField) {
                reporter.reportOn(source, FirSerializationErrors.TRANSIENT_IS_REDUNDANT)
            } else if (!isInitialized) {
                reporter.reportOn(source, FirSerializationErrors.TRANSIENT_MISSING_INITIALIZER)
            }
        }
    }

    private fun declarationHasInitializer(propertySymbol: FirPropertySymbol): Boolean {
        return when {
            propertySymbol.fromPrimaryConstructor -> propertySymbol.correspondingValueParameterFromPrimaryConstructor?.hasDefaultValue ?: false
            else -> propertySymbol.hasInitializer || propertySymbol.hasDelegate
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun analyzePropertiesSerializers(
        classSymbol: FirClassSymbol<*>,
        properties: List<FirSerializableProperty>,
        reporter: DiagnosticReporter
    ) {
        for (property in properties) {
            // TODO: reporting diagnostics on properties from superclasses looks a bad idea
            val serializerType = property.serializableWith
            val serializerSymbol = serializerType?.toRegularClassSymbol(session)
            val propertySymbol = property.propertySymbol
            val typeRef = propertySymbol.resolvedReturnTypeRef
            val propertyType = typeRef.coneType
            val source = typeRef.source ?: propertySymbol.source
            if (serializerType != null && serializerSymbol != null) {
                checkCustomSerializerMatch(
                    classSymbol,
                    source = typeRef.source ?: propertySymbol.source,
                    propertyType,
                    serializerType,
                    reporter
                )
                checkCustomSerializerIsNotLocal(
                    source = propertySymbol.serializableAnnotation(needArguments = false)?.source,
                    classSymbol,
                    serializerType,
                    reporter
                )
                checkSerializerNullability(propertyType, serializerType, source, reporter)
            } else {
                checkType(propertyType, source, reporter)
            }
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkTypeArguments(type: ConeKotlinType, source: KtSourceElement?, reporter: DiagnosticReporter) {
        for (typeArgument in type.typeArguments) {
            checkType(
                typeArgument.type ?: continue,
                source,
                reporter
            )
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun canSupportInlineClasses(): Boolean {
        return session.versionReader.canSupportInlineClasses
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private val ConeKotlinType.isUnsupportedInlineType: Boolean
        get() = isInlineClass(session) && !isPrimitiveOrNullablePrimitive

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkType(type: ConeKotlinType, source: KtSourceElement?, reporter: DiagnosticReporter) {
        if (type.lowerBoundIfFlexible().isTypeParameter) return // type parameters always have serializer stored in class' field
        if (type.isUnsupportedInlineType && !canSupportInlineClasses()) {
            reporter.reportOn(
                source,
                FirSerializationErrors.INLINE_CLASSES_NOT_SUPPORTED,
                RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES.toString(),
                session.versionReader.runtimeVersions?.implementationVersion.toString()
            )
        }

        val serializer = findTypeSerializerOrContextUnchecked(type)
        if (serializer != null) {
            val classSymbol = type.toRegularClassSymbol(session) ?: return
            type.serializableWith?.fullyExpandedType(session)?.let { serializerType ->
                // TODO: get element of type
                checkCustomSerializerMatch(classSymbol, source, type, serializerType, reporter)
                checkCustomSerializerIsNotLocal(source, classSymbol, serializerType, reporter)
                checkSerializerNullability(serializer.defaultType(), serializerType, source, reporter)
            }
            checkTypeArguments(type, source, reporter)
        } else {
            reporter.reportOn(source, FirSerializationErrors.SERIALIZER_NOT_FOUND, type)
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkCustomSerializerMatch(
        containingClassSymbol: FirClassSymbol<*>,
        source: KtSourceElement?,
        declarationType: ConeKotlinType,
        serializerType: ConeKotlinType,
        reporter: DiagnosticReporter
    ) {
        val serializerForType = serializerType.serializerForType(session) ?: return

        val declarationTypeClassId = declarationType.classId
        if (declarationTypeClassId == null || declarationTypeClassId != serializerForType.classId) {
            reporter.reportOn(
                source ?: containingClassSymbol.serializableOrMetaAnnotationSource,
                FirSerializationErrors.SERIALIZER_TYPE_INCOMPATIBLE,
                containingClassSymbol.defaultType(),
                serializerType,
                serializerForType
            )
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkCustomSerializerIsNotLocal(
        source: KtSourceElement?,
        classSymbol: FirClassSymbol<*>,
        serializerType: ConeKotlinType,
        reporter: DiagnosticReporter
    ) {
        val serializerClassId = serializerType.classId ?: return
        if (serializerClassId.isLocal) {
            reporter.reportOn(source ?: classSymbol.serializableOrMetaAnnotationSource, FirSerializationErrors.LOCAL_SERIALIZER_USAGE, serializerType)
        }
    }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun checkSerializerNullability(
        classType: ConeKotlinType,
        serializerType: ConeKotlinType,
        source: KtSourceElement?,
        reporter: DiagnosticReporter
    ) {
        // @Serializable annotation has proper signature so this error would be caught in type checker
        val serializerForType = serializerType.serializerForType(session) ?: return
        if (!classType.isMarkedNullable && serializerForType.isMarkedNullable) {
            reporter.reportOn(source, FirSerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE, serializerType, classType)
        }
    }

    // --------------------------------------------------------------------------------------

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private val FirClassSymbol<*>.isSerializableEnumWithMissingSerializer: Boolean
        get() {
            if (!isEnumClass) return false
            if (hasSerializableOrMetaAnnotation) return false
            if (hasAnySerialAnnotation) return true
            return collectEnumEntries().any { it.hasAnySerialAnnotation }
        }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private val FirClassSymbol<*>.serializableOrMetaAnnotationSource: KtSourceElement?
        get() {
            serializableAnnotation(needArguments = false)?.source?.let { return it }
            metaSerializableAnnotation(needArguments = false)?.source?.let { return it }
            return null
        }

    context(CheckerContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private val FirClassSymbol<*>.serializableAnnotationIsUseless: Boolean
        get() = !classKind.isEnumClass &&
                hasSerializableOrMetaAnnotationWithoutArgs &&
                !isInternalSerializable &&
                !hasCompanionObjectAsSerializer &&
                !isSealedSerializableInterface
}
