/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.fileNameForPluginGeneratedCallable
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

public class PropertyBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    private val callableId: CallableId,
    private val returnTypeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
    private val isVal: Boolean,
    private val hasBackingField: Boolean,
    private val containingFileName: String?,
) : DeclarationBuildingContext<FirProperty>(session, key, owner) {
    private var setterVisibility: Visibility? = null
    private var extensionReceiverTypeProvider: ((List<FirTypeParameter>) -> ConeKotlinType)? = null

    /**
     * Sets [type] as extension receiver type of constructed property
     */
    public fun extensionReceiverType(type: ConeKotlinType) {
        extensionReceiverType { type }
    }

    /**
     * Sets type, provided by [typeProvider], as extension receiver type of constructed property
     *
     * Use this overload when extension receiver type uses type parameters of constructed property
     */
    public fun extensionReceiverType(typeProvider: (List<FirTypeParameter>) -> ConeKotlinType) {
        extensionReceiverTypeProvider = typeProvider
    }

    /**
     * Declares [visibility] of property setter if property marked as var
     * If this function is not called then setter will have same visibility
     *   as property itself
     */
    public fun setter(visibility: Visibility) {
        setterVisibility = visibility
    }

    override fun build(): FirProperty {
        return buildProperty {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            source = getSourceForFirDeclaration()

            symbol = FirRegularPropertySymbol(callableId)
            name = callableId.callableName

            val resolvedStatus = generateStatus()
            status = resolvedStatus

            dispatchReceiverType = owner?.defaultType()

            this@PropertyBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }
            initTypeParameterBounds(typeParameters, typeParameters)

            returnTypeRef = returnTypeProvider.invoke(typeParameters).toFirResolvedTypeRef()
            extensionReceiverTypeProvider?.invoke(typeParameters)?.let {
                receiverParameter = buildReceiverParameter {
                    typeRef = it.toFirResolvedTypeRef()
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = key.origin
                    containingDeclarationSymbol = this@buildProperty.symbol
                }
            }

            produceContextReceiversTo(contextParameters, typeParameters, origin, symbol)

            isVar = !isVal
            getter = FirDefaultPropertyGetter(
                source = null,
                moduleData = session.moduleData,
                origin = key.origin,
                propertyTypeRef = returnTypeRef,
                visibility = status.visibility,
                propertySymbol = symbol,
                modality = resolvedStatus.modality,
                effectiveVisibility = resolvedStatus.effectiveVisibility,
                resolvePhase = FirResolvePhase.BODY_RESOLVE,
            )
            if (isVar) {
                setter = FirDefaultPropertySetter(
                    source = null,
                    moduleData = session.moduleData,
                    origin = key.origin,
                    propertyTypeRef = returnTypeRef,
                    visibility = setterVisibility ?: status.visibility,
                    propertySymbol = symbol,
                    modality = resolvedStatus.modality,
                    effectiveVisibility = resolvedStatus.effectiveVisibility,
                    resolvePhase = FirResolvePhase.BODY_RESOLVE,
                )
            } else {
                require(setterVisibility == null) { "isVar = false but setterVisibility is specified. Did you forget to set isVar = true?" }
            }

            if (hasBackingField) {
                backingField = FirDefaultPropertyBackingField(
                    session.moduleData,
                    key.origin,
                    source = null,
                    mutableListOf(),
                    returnTypeRef,
                    isVar,
                    symbol,
                    status,
                    resolvePhase = FirResolvePhase.BODY_RESOLVE,
                )
            }
            bodyResolveState = FirPropertyBodyResolveState.ALL_BODIES_RESOLVED
        }.also {
            if (containingFileName != null) {
                require(callableId.classId == null) { "containingFileName could be set only for top-level declarations, but $callableId is a member" }
            }
            it.fileNameForPluginGeneratedCallable = containingFileName
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Creates a member property for [owner] class with [returnType] return type
 */
public fun FirExtension.createMemberProperty(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    return createMemberProperty(owner, key, name, { returnType }, isVal, hasBackingField, config)
}

/**
 * Creates a member property for [owner] class with return type provided by [returnTypeProvider]
 * Use this overload when those types use type parameters of constructed property
 */
public fun FirExtension.createMemberProperty(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnTypeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    val callableId = CallableId(owner.classId, name)
    return PropertyBuildingContext(
        session, key, owner, callableId, returnTypeProvider, isVal, hasBackingField,
        containingFileName = null
    ).apply(config).apply {
        status {
            isExpect = owner.isExpect
        }
    }.build()
}

/**
 * Creates a top-level property class with [returnType] return type
 *
 * If you create top-level extension property don't forget to set [hasBackingField] to false,
 *   since such properties never have backing fields
 *
 * @param containingFileName defines the name for a newly created file with this property.
 * The full file path would be `package/of/the/property/containingFileName.kt.
 * If null is passed, then `__GENERATED BUILTINS DECLARATIONS__.kt` would be used
 */
@ExperimentalTopLevelDeclarationsGenerationApi
public fun FirExtension.createTopLevelProperty(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnType: ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    containingFileName: String? = null,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    return createTopLevelProperty(key, callableId, { returnType }, isVal, hasBackingField, containingFileName, config)
}

/**
 * Creates a top-level property with return type provided by [returnTypeProvider]
 *
 * If you create top-level extension property don't forget to set [hasBackingField] to false,
 *   since such properties never have backing fields
 *
 * Use this overload when those types use type parameters of constructed property
 *
 * @param containingFileName defines the name for a newly created file with this property.
 * The full file path would be `package/of/the/property/containingFileName.kt.
 * If null is passed, then `__GENERATED BUILTINS DECLARATIONS__.kt` would be used
 */
@ExperimentalTopLevelDeclarationsGenerationApi
public fun FirExtension.createTopLevelProperty(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnTypeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    containingFileName: String? = null,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    require(callableId.classId == null)
    return PropertyBuildingContext(
        session, key, owner = null, callableId, returnTypeProvider, isVal, hasBackingField, containingFileName
    ).apply(config).build()
}
