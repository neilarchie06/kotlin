/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.mpp.TypeRefMarker

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.resolvedTypeRef]
 */
abstract class FirResolvedTypeRef : FirTypeRef(), TypeRefMarker {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val customRenderer: Boolean
    abstract val coneType: ConeKotlinType
    abstract val delegatedTypeRef: FirTypeRef?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformResolvedTypeRef(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedTypeRef
}
