/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.utils.toIrSymbol
import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.unexpandedConeClassLikeType
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ExtensionFunctionType
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.NoInfer
import org.jetbrains.kotlin.types.CommonFlexibleTypeBoundsChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils

class Fir2IrTypeConverter(
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by c {

    internal val classIdToSymbolMap by lazy {
        // Note: this map must include all base classes, and they should be before derived classes!
        mapOf(
            StandardClassIds.Nothing to builtins.nothingClass,
            StandardClassIds.Any to builtins.anyClass,
            StandardClassIds.Unit to builtins.unitClass,
            StandardClassIds.Boolean to builtins.booleanClass,
            StandardClassIds.CharSequence to builtins.charSequenceClass,
            StandardClassIds.String to builtins.stringClass,
            StandardClassIds.Number to builtins.numberClass,
            StandardClassIds.Long to builtins.longClass,
            StandardClassIds.Int to builtins.intClass,
            StandardClassIds.Short to builtins.shortClass,
            StandardClassIds.Byte to builtins.byteClass,
            StandardClassIds.Float to builtins.floatClass,
            StandardClassIds.Double to builtins.doubleClass,
            StandardClassIds.Char to builtins.charClass,
            StandardClassIds.Array to builtins.arrayClass,
        )
    }

    internal val classIdToTypeMap by lazy {
        mapOf(
            StandardClassIds.Nothing to builtins.nothingType,
            StandardClassIds.Unit to builtins.unitType,
            StandardClassIds.Boolean to builtins.booleanType,
            StandardClassIds.String to builtins.stringType,
            StandardClassIds.Any to builtins.anyType,
            StandardClassIds.Long to builtins.longType,
            StandardClassIds.Int to builtins.intType,
            StandardClassIds.Short to builtins.shortType,
            StandardClassIds.Byte to builtins.byteType,
            StandardClassIds.Float to builtins.floatType,
            StandardClassIds.Double to builtins.doubleType,
            StandardClassIds.Char to builtins.charType
        )
    }

    private val capturedTypeCache = mutableMapOf<ConeCapturedType, IrType>()
    private val errorTypeForCapturedTypeStub by lazy { createErrorType() }

    fun FirTypeRef.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType {
        capturedTypeCache.clear()
        return when (this) {
            !is FirResolvedTypeRef -> createErrorType()
            !is FirImplicitBuiltinTypeRef -> coneType.toIrType(typeOrigin, annotations)
            is FirImplicitNothingTypeRef -> builtins.nothingType
            is FirImplicitUnitTypeRef -> builtins.unitType
            is FirImplicitBooleanTypeRef -> builtins.booleanType
            is FirImplicitStringTypeRef -> builtins.stringType
            is FirImplicitAnyTypeRef -> builtins.anyType
            is FirImplicitIntTypeRef -> builtins.intType
            is FirImplicitNullableAnyTypeRef -> builtins.anyNType
            is FirImplicitNullableNothingTypeRef -> builtins.nothingNType
            else -> coneType.toIrType(typeOrigin, annotations)
        }
    }

    fun ConeKotlinType.toIrType(
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
        annotations: List<FirAnnotation> = emptyList(),
        hasFlexibleNullability: Boolean = false,
        hasFlexibleMutability: Boolean = false,
        hasFlexibleArrayElementVariance: Boolean = false,
        addRawTypeAnnotation: Boolean = false
    ): IrType {
        return when (val type = fullyExpandedType()) {
            is ConeErrorType -> {
                when (val diagnostic = type.diagnostic) {
                    is ConeUnresolvedError -> createErrorType(diagnostic.qualifier, type.isMarkedNullable)
                    else -> createErrorType(diagnostic.reason, type.isMarkedNullable)
                }
            }
            is ConeLookupTagBasedType -> {
                val typeAnnotations = mutableListOf<IrConstructorCall>()
                typeAnnotations += with(annotationGenerator) { annotations.toIrAnnotations() }

                val irSymbol =
                    getBuiltInClassSymbol(type.classId)
                        ?: type.lookupTag.toSymbol(session)?.let { firSymbol ->
                            approximateTypeForLocalClassIfNeeded(firSymbol)?.let { return it }
                            firSymbol.toIrSymbol(typeOrigin) {
                                typeAnnotations += with(annotationGenerator) { it.toIrAnnotations() }
                            }
                        }
                        ?: (type.lookupTag as? ConeClassLikeLookupTag)?.let(classifierStorage::getIrClassForNotFoundClass)?.symbol
                        ?: return createErrorType()

                val specialAnnotationsProvider = specialAnnotationsProvider
                if (specialAnnotationsProvider != null) {
                    if (type.hasEnhancedNullability) {
                        typeAnnotations += specialAnnotationsProvider.generateEnhancedNullabilityAnnotationCall()
                    }
                    if (hasFlexibleNullability) {
                        typeAnnotations += specialAnnotationsProvider.generateFlexibleNullabilityAnnotationCall()
                    }
                    if (hasFlexibleMutability) {
                        typeAnnotations += specialAnnotationsProvider.generateFlexibleMutabilityAnnotationCall()
                    }
                    if (hasFlexibleArrayElementVariance) {
                        typeAnnotations += specialAnnotationsProvider.generateFlexibleArrayElementVarianceAnnotationCall()
                    }
                    if (addRawTypeAnnotation) {
                        typeAnnotations += specialAnnotationsProvider.generateRawTypeAnnotationCall()
                    }
                }

                if (type.isExtensionFunctionType && annotations.getAnnotationsByClassId(ExtensionFunctionType, session).isEmpty()) {
                    builtins.extensionFunctionTypeAnnotationCall?.let {
                        typeAnnotations += it
                    }
                }

                if (type.hasNoInfer && annotations.getAnnotationsByClassId(NoInfer, session).isEmpty()) {
                    builtins.noInferAnnotationCall?.let {
                        typeAnnotations += it
                    }
                }

                for (attributeAnnotation in type.typeAnnotations) {
                    val isAlreadyPresentInAnnotations = annotations.any {
                        it.unexpandedConeClassLikeType == attributeAnnotation.unexpandedConeClassLikeType
                    }
                    if (isAlreadyPresentInAnnotations) continue
                    typeAnnotations += callGenerator.convertToIrConstructorCall(attributeAnnotation) as? IrConstructorCall ?: continue
                }
                val approximatedType = type.approximateForIrOrSelf()

                if (approximatedType is ConeTypeParameterType && conversionScope.shouldEraseType(approximatedType)) {
                    // This hack is about type parameter leak in case of generic delegated property
                    // It probably will be prohibited after 2.0
                    // For more details see KT-24643
                    return approximateUpperBounds(approximatedType.lookupTag.typeParameterSymbol.resolvedBounds)
                }

                IrSimpleTypeImpl(
                    irSymbol,
                    hasQuestionMark = approximatedType.isMarkedNullable,
                    arguments = approximatedType.typeArguments.map { it.toIrTypeArgument(typeOrigin) },
                    annotations = typeAnnotations
                )
            }
            is ConeRawType -> {
                // Upper bound has star projections here, so we take lower one
                // (some reflection tests rely on this)
                type.lowerBound.withNullabilityOf(type.upperBound, session.typeContext).toIrType(
                    typeOrigin,
                    annotations,
                    hasFlexibleNullability = type.lowerBound.isMarkedNullable != type.upperBound.isMarkedNullable,
                    hasFlexibleMutability = type.isMutabilityFlexible(),
                    hasFlexibleArrayElementVariance = false,
                    addRawTypeAnnotation = true
                )
            }
            is ConeDynamicType -> {
                val typeAnnotations = with(annotationGenerator) { annotations.toIrAnnotations() }
                return IrDynamicTypeImpl(typeAnnotations, Variance.INVARIANT)
            }
            is ConeFlexibleType -> with(session.typeContext) {
                if (type.upperBound is ConeClassLikeType) {
                    val upper = type.upperBound as ConeClassLikeType
                    val lower = type.lowerBound
                    val isRaw = type.attributes.contains(CompilerConeAttributes.RawType)
                    val intermediate = if (lower is ConeClassLikeType && lower.lookupTag == upper.lookupTag && !isRaw) {
                        lower.replaceArguments(upper.getArguments())
                    } else lower
                    (intermediate.withNullability(upper.isMarkedNullable) as ConeKotlinType)
                        .withAttributes(type.attributes)
                        .toIrType(
                            typeOrigin,
                            annotations,
                            hasFlexibleNullability = lower.isMarkedNullable != upper.isMarkedNullable,
                            hasFlexibleMutability = type.isMutabilityFlexible(),
                            hasFlexibleArrayElementVariance = type.hasFlexibleArrayElementVariance(),
                            addRawTypeAnnotation = isRaw,
                        )
                } else {
                    type.upperBound.toIrType(
                        typeOrigin,
                        annotations,
                        hasFlexibleNullability = type.lowerBound.isMarkedNullable != type.upperBound.isMarkedNullable,
                        hasFlexibleMutability = type.isMutabilityFlexible(),
                        hasFlexibleArrayElementVariance = false,
                        addRawTypeAnnotation = false,
                    )
                }
            }
            is ConeCapturedType -> {
                val cached = capturedTypeCache[type]
                if (cached == null) {
                    capturedTypeCache[type] = errorTypeForCapturedTypeStub
                    val irType = type.approximateForIrOrSelf().toIrType(
                        typeOrigin,
                        annotations,
                        hasFlexibleNullability = hasFlexibleNullability,
                        hasFlexibleMutability = hasFlexibleMutability,
                        addRawTypeAnnotation = addRawTypeAnnotation
                    )
                    capturedTypeCache[type] = irType
                    irType
                } else {
                    // Potentially recursive captured type, e.g., Recursive<R> where R : Recursive<R>, ...
                    // That should have been handled during type argument conversion, though.
                    // Or, simply repeated captured type, e.g., FunctionN<..., *, ..., *>, literally same captured types.
                    cached
                }
            }
            is ConeDefinitelyNotNullType -> {
                type.original.toIrType(typeOrigin).makeNotNull()
            }
            is ConeIntersectionType -> {
                val approximated = type.approximateForIrOrNull()!!
                approximated.toIrType(typeOrigin)
            }
            is ConeStubType, is ConeIntegerLiteralType, is ConeTypeVariableType -> createErrorType()
        }
    }

    private fun ConeFlexibleType.hasFlexibleArrayElementVariance(): Boolean =
        lowerBound.let { lowerBound ->
            lowerBound.classLikeLookupTagIfAny?.classId == StandardClassIds.Array &&
                    lowerBound.typeArguments.single().kind == ProjectionKind.INVARIANT
        } && upperBound.let { upperBound ->
            upperBound.classLikeLookupTagIfAny?.classId == StandardClassIds.Array &&
                    upperBound.typeArguments.single().kind == ProjectionKind.OUT
        }

    private fun approximateUpperBounds(resolvedBounds: List<FirResolvedTypeRef>): IrType {
        val commonSupertype = session.typeContext.commonSuperTypeOrNull(resolvedBounds.map { it.coneType })!!.lowerBoundIfFlexible()
        val resultType = (commonSupertype as? ConeClassLikeType)?.replaceArgumentsWithStarProjections()
            ?: commonSupertype
        val approximatedType = (commonSupertype as? ConeSimpleKotlinType)?.approximateForIrOrSelf() ?: resultType
        return approximatedType.toIrType()
    }

    private fun ConeFlexibleType.isMutabilityFlexible(): Boolean {
        val lowerFqName = lowerBound.classId?.asSingleFqName() ?: return false
        val upperFqName = upperBound.classId?.asSingleFqName() ?: return false
        if (lowerFqName == upperFqName) return false
        return CommonFlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(lowerFqName) ==
                CommonFlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(upperFqName)
    }

    private fun ConeTypeProjection.toIrTypeArgument(typeOrigin: ConversionTypeOrigin): IrTypeArgument {
        fun toIrTypeArgument(type: ConeKotlinType, variance: Variance): IrTypeProjection {
            val irType = type.toIrType(typeOrigin)
            return makeTypeProjection(irType, variance)
        }

        return when (this) {
            ConeStarProjection -> IrStarProjectionImpl
            is ConeKotlinTypeProjectionIn -> toIrTypeArgument(this.type, Variance.IN_VARIANCE)
            is ConeKotlinTypeProjectionOut -> toIrTypeArgument(this.type, Variance.OUT_VARIANCE)
            is ConeKotlinTypeConflictingProjection -> toIrTypeArgument(this.type, Variance.INVARIANT)
            is ConeKotlinType -> {
                if (this is ConeCapturedType && this in capturedTypeCache && this.isRecursive(mutableSetOf())) {
                    // Recursive captured type, e.g., Recursive<R> where R : Recursive<R>, ...
                    // We can return * early here to avoid recursive type conversions.
                    IrStarProjectionImpl
                } else {
                    val irType = toIrType(typeOrigin)
                    makeTypeProjection(irType, Variance.INVARIANT)
                }
            }
        }
    }

    private fun ConeKotlinType.isRecursive(visited: MutableSet<ConeCapturedType>): Boolean =
        when (this) {
            is ConeLookupTagBasedType -> {
                typeArguments.any {
                    when (it) {
                        is ConeKotlinType -> it.isRecursive(visited)
                        is ConeKotlinTypeProjectionIn -> it.type.isRecursive(visited)
                        is ConeKotlinTypeProjectionOut -> it.type.isRecursive(visited)
                        else -> false
                    }
                }
            }
            is ConeFlexibleType -> {
                lowerBound.isRecursive(visited) || upperBound.isRecursive(visited)
            }
            is ConeCapturedType -> {
                if (visited.add(this)) {
                    constructor.supertypes?.any { it.isRecursive(visited) } == true
                } else
                    true
            }
            is ConeDefinitelyNotNullType -> {
                original.isRecursive(visited)
            }
            is ConeIntersectionType -> {
                intersectedTypes.any { it.isRecursive(visited) }
            }
            else -> false
        }

    private fun getArrayClassSymbol(classId: ClassId?): IrClassSymbol? {
        val primitiveId = StandardClassIds.elementTypeByPrimitiveArrayType[classId] ?: return null
        val irType = classIdToTypeMap[primitiveId]
        return builtins.primitiveArrayForType[irType] ?: error("Strange primitiveId $primitiveId from array: $classId")
    }

    // TODO: candidate for removal
    private fun getBuiltInClassSymbol(classId: ClassId?): IrClassSymbol? {
        return classIdToSymbolMap[classId] ?: getArrayClassSymbol(classId)
    }

    private fun approximateTypeForLocalClassIfNeeded(symbol: FirClassifierSymbol<*>): IrType? {
        // Should only be run in the skipBodies (i.e. kapt) mode.
        if (!configuration.skipBodies) return null

        if (symbol !is FirClassSymbol) return null
        val firClass = symbol.fir
        if (!firClass.isLocalClassOrAnonymousObject()) return null
        return firClass.superTypeRefs.firstOrNull {
            // Skip Enum supertype because otherwise, translating local enums will lead to stack overflow error
            // (since a local enum `L` has `kotlin/Enum<L>` as a supertype).
            (it.coneType.lookupTagIfAny as? ConeClassLikeLookupTag)?.classId != StandardClassIds.Enum
        }?.toIrType() ?: builtins.anyType
    }
}

fun FirTypeRef.toIrType(
    typeConverter: Fir2IrTypeConverter,
    typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
): IrType {
    return with(typeConverter) {
        toIrType(typeOrigin)
    }
}

context(c: Fir2IrComponents)
fun FirTypeRef.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType {
    return with(c.typeConverter) {
        toIrType(typeOrigin)
    }
}

context(c: Fir2IrComponents)
fun ConeKotlinType.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType {
    return with(c.typeConverter) {
        toIrType(typeOrigin)
    }
}

context(c: Fir2IrComponents)
internal fun ConeKotlinType.approximateForIrOrNull(): ConeKotlinType? {
    return c.session.typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.FrontendToBackendTypesApproximation)
}

context(c: Fir2IrComponents)
internal fun ConeKotlinType.approximateForIrOrSelf(): ConeKotlinType {
    return approximateForIrOrNull() ?: this
}

internal fun createErrorType(message: String = "<error>", isMarkedNullable: Boolean = false): IrErrorType =
    IrErrorTypeImpl(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, message), emptyList(), Variance.INVARIANT, isMarkedNullable)

context(c: Fir2IrComponents)
fun ConeKotlinType.approximateFunctionTypeInputs(): ConeKotlinType {
    // Approximate a function type's input types to their supertypes.
    // Approximating the outer type will lead to the input types being approximated to their subtypes
    // because the input type parameters have in variance.
    if (this !is ConeClassLikeType) return this

    val typeArguments = typeArguments
    return this.withArguments(Array(typeArguments.size) { i ->
        val projection = typeArguments[i]
        if (i < typeArguments.lastIndex) {
            projection.type?.approximateForIrOrNull()?.toTypeProjection(projection.kind) ?: projection
        } else {
            projection
        }
    })
}