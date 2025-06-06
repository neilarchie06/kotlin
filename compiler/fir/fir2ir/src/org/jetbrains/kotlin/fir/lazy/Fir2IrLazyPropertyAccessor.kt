/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.lazyMappedFunctionListVar
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyPropertyAccessor(
    c: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    private val firAccessor: FirPropertyAccessor?,
    val isSetter: Boolean,
    private val firParentProperty: FirProperty,
    firParentClass: FirRegularClass?,
    symbol: IrSimpleFunctionSymbol,
    parent: IrDeclarationParent,
    isFakeOverride: Boolean,
    override var correspondingPropertySymbol: IrPropertySymbol?
) : AbstractFir2IrLazyFunction<FirCallableDeclaration>(c, startOffset, endOffset, origin, symbol, parent, isFakeOverride) {
    init {
        symbol.bind(this)
    }

    override val fir: FirCallableDeclaration
        get() = firAccessor ?: firParentProperty

    // TODO: investigate why some deserialized properties are inline
    override var isInline: Boolean
        get() = firAccessor?.isInline == true
        set(_) = mutationNotSupported()

    override var annotations: List<IrConstructorCall> by when {
        firAccessor != null -> createLazyAnnotations()
        else -> lazyVar<List<IrConstructorCall>>(lock) { emptyList() }
    }

    override var name: Name
        get() = Name.special("<${if (isSetter) "set" else "get"}-${firParentProperty.name}>")
        set(_) = mutationNotSupported()

    override var returnType: IrType by lazyVar(lock) {
        if (isSetter) builtins.unitType else firParentProperty.returnTypeRef.toIrType(conversionTypeContext)
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by symbolsMappingForLazyClasses.lazyMappedFunctionListVar(lock) {
        if (firParentClass == null) return@lazyMappedFunctionListVar emptyList()
        // If property accessor is created then corresponding property is definitely created too
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val correspondingProperty = correspondingPropertySymbol!!.owner
        correspondingProperty.overriddenSymbols.mapNotNull {
            /*
             * Calculation of overridden symbols for lazy accessor may be called
             * 1. during fir2ir transformation
             * 2. somewhere in the backend, after fake-overrides were built
             *
             * In the first case declarationStorage knows about all symbols, so we always can search for accessor symbol in it
             * But in the second case property symbols for fake-overrides are replaced with real one (in f/o generator) and
             *   declarationStorage has no information about it. But at this point all symbols are already bound. So we can
             *   just access the corresponding accessor using owner of the symbol
             */
            when {
                it.isBound -> @OptIn(UnsafeDuringIrConstructionAPI::class) when (isSetter) {
                    false -> it.owner.getter?.symbol
                    true -> it.owner.setter?.symbol
                }
                else -> when (isSetter) {
                    false -> declarationStorage.findGetterOfProperty(it)
                    true -> declarationStorage.findSetterOfProperty(it)
                }
            }
        }
    }

    override val initialSignatureFunction: IrFunction? by lazy {
        val originalFirFunction = (fir as? FirSyntheticPropertyAccessor)?.delegate ?: return@lazy null
        // If property accessor is created then corresponding property is definitely created too
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val lookupTag = (correspondingPropertySymbol!!.owner as Fir2IrLazyProperty).containingClass?.symbol?.toLookupTag()

        // `initialSignatureFunction` is not called during fir2ir conversion
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        declarationStorage.getIrFunctionSymbol(originalFirFunction.symbol, lookupTag).owner
    }

    override val containerSource: DeserializedContainerSource?
        get() = firParentProperty.containerSource

    private val conversionTypeContext = if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
}
