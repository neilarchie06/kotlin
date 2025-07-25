/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.actualizer.checker.areIrExpressionConstValuesEqual
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext.AnnotationCallInfo
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext.Companion.abstractMutableListModCountCallableId
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal abstract class IrExpectActualMatchingContext(
    val typeContext: IrTypeSystemContext,
    val expectToActualClassMap: ClassActualizationInfo.ActualClassMapping
) : ExpectActualMatchingContext<IrSymbol>, TypeSystemContext by typeContext {
    // Default params are not checked on backend because backend ignores expect classes in fake override builder.
    // See https://github.com/JetBrains/kotlin/commit/8d725753f8f8d430101a17bc1049463a6319359b
    // Default params can't be accurately checked without information about overriddenSymbols of expect classes members
    override val shouldCheckDefaultParams: Boolean
        get() = false

    private inline fun <R> CallableSymbolMarker.processIr(
        onFunction: (IrFunction) -> R,
        onProperty: (IrProperty) -> R,
        onField: (IrField) -> R,
        onValueParameter: (IrValueParameter) -> R,
        onEnumEntry: (IrEnumEntry) -> R,
    ): R {
        return when (this) {
            is IrFunctionSymbol -> onFunction(owner)
            is IrPropertySymbol -> onProperty(owner)
            is IrFieldSymbol -> onField(owner)
            is IrValueParameterSymbol -> onValueParameter(owner)
            is IrEnumEntrySymbol -> onEnumEntry(owner)
            else -> error("Unsupported declaration: $this")
        }
    }

    private inline fun <R> ClassLikeSymbolMarker.processIr(
        onClass: (IrClass) -> R,
        onTypeAlias: (IrTypeAlias) -> R,
    ): R {
        return when (this) {
            is IrClassSymbol -> onClass(owner)
            is IrTypeAliasSymbol -> onTypeAlias(owner)
            else -> error("Unsupported declaration: $this")
        }
    }

    private fun DeclarationSymbolMarker.asIr(): IrDeclaration = (this as IrSymbol).owner as IrDeclaration
    private fun FunctionSymbolMarker.asIr(): IrFunction = (this as IrFunctionSymbol).owner
    private fun PropertySymbolMarker.asIr(): IrProperty = (this as IrPropertySymbol).owner
    private fun ValueParameterSymbolMarker.asIr(): IrValueParameter = (this as IrValueParameterSymbol).owner
    private fun TypeParameterSymbolMarker.asIr(): IrTypeParameter = (this as IrTypeParameterSymbol).owner
    private fun RegularClassSymbolMarker.asIr(): IrClass = (this as IrClassSymbol).owner
    private fun TypeAliasSymbolMarker.asIr(): IrTypeAlias = (this as IrTypeAliasSymbol).owner

    private inline fun <reified T : IrDeclaration> DeclarationSymbolMarker.safeAsIr(): T? = (this as IrSymbol).owner as? T

    override val innerClassesCapturesOuterTypeParameters: Boolean
        get() = false

    override val RegularClassSymbolMarker.classId: ClassId
        get() = asIr().classIdOrFail

    override val TypeAliasSymbolMarker.classId: ClassId
        get() = asIr().classIdOrFail

    override val CallableSymbolMarker.callableId: CallableId
        get() = processIr(
            onFunction = { it.callableId },
            onProperty = { it.callableId },
            onField = { it.callableId },
            onValueParameter = { shouldNotBeCalled() },
            onEnumEntry = { it.callableId }
        )

    override val TypeParameterSymbolMarker.parameterName: Name
        get() = asIr().name
    override val ValueParameterSymbolMarker.parameterName: Name
        get() = asIr().name

    override fun TypeAliasSymbolMarker.expandToRegularClass(): RegularClassSymbolMarker? {
        return asIr().expandedType.getClass()?.symbol
    }

    override val RegularClassSymbolMarker.classKind: ClassKind
        get() = asIr().kind
    override val RegularClassSymbolMarker.isCompanion: Boolean
        get() = asIr().isCompanion
    override val RegularClassSymbolMarker.isInner: Boolean
        get() = asIr().isInner
    override val RegularClassSymbolMarker.isInlineOrValue: Boolean
        get() = asIr().isValue

    override val RegularClassSymbolMarker.isFun: Boolean
        get() = asIr().isFun

    override val ClassLikeSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() {
            val parameters = processIr(
                onClass = { it.typeParameters },
                onTypeAlias = { it.typeParameters },
            )
            return parameters.map { it.symbol }
        }

    override val ClassLikeSymbolMarker.modality: Modality
        get() = processIr(
            onClass = {
                when {
                    // Modality for enum classes is not trivial in IR.
                    // That's why we need to "normalize" modality for the expect-actual checker
                    // See FirRegularClass.enumClassModality & 940567b8bd72f69b3eb7d54ff780f98a17e6b9fc
                    it.isEnumClass -> Modality.FINAL
                    // For some reason kotlin annotations in IR have open modality and java annotations have final modality
                    // But since it's legal to actualize kotlin annotation class with java annotation class
                    //  and effectively all annotation classes have the same modality, it's ok to always return one
                    //  modality for all annotation classes (doesn't matter final or open)
                    it.isAnnotationClass -> Modality.OPEN
                    else -> it.modality
                }
            },
            onTypeAlias = { Modality.FINAL }
        )
    override val ClassLikeSymbolMarker.visibility: Visibility
        get() = safeAsIr<IrDeclarationWithVisibility>()!!.visibility.delegate

    override val CallableSymbolMarker.modality: Modality?
        get() = when (this) {
            // owner.modality is null for IrEnumEntrySymbol
            is IrEnumEntrySymbol, is IrConstructorSymbol -> Modality.FINAL
            is IrSymbol -> (owner as? IrOverridableMember)?.modality
            else -> shouldNotBeCalled()
        }

    override val CallableSymbolMarker.visibility: Visibility
        get() = when (this) {
            is IrEnumEntrySymbol -> Visibilities.Public
            is IrSymbol -> (owner as IrDeclarationWithVisibility).visibility.delegate
            else -> shouldNotBeCalled()
        }

    override val RegularClassSymbolMarker.superTypes: List<IrType>
        get() = asIr().superTypes

    override val RegularClassSymbolMarker.superTypesRefs: List<TypeRefMarker>
        get() = superTypes

    override val RegularClassSymbolMarker.defaultType: KotlinTypeMarker
        get() = asIr().defaultType

    override val CallableSymbolMarker.isExpect: Boolean
        get() = processIr(
            onFunction = { it.isExpect },
            onProperty = { it.isExpect },
            onField = { false },
            onValueParameter = { false },
            onEnumEntry = { false }
        )
    override val CallableSymbolMarker.isInline: Boolean
        get() = processIr(
            onFunction = { it.isInline },
            onProperty = { false }, // property can not be inline in IR. Its getter is inline instead
            onField = { false },
            onValueParameter = { false },
            onEnumEntry = { false }
        )

    override val CallableSymbolMarker.isSuspend: Boolean
        get() = processIr(
            onFunction = { it.isSuspend },
            onProperty = { false }, // property can not be suspend in IR. Its getter is suspend instead
            onField = { false },
            onValueParameter = { false },
            onEnumEntry = { false }
        )

    override val CallableSymbolMarker.isExternal: Boolean
        get() = safeAsIr<IrPossiblyExternalDeclaration>()?.isExternal ?: false

    override val CallableSymbolMarker.isInfix: Boolean
        get() = safeAsIr<IrSimpleFunction>()?.isInfix ?: false

    override val CallableSymbolMarker.isOperator: Boolean
        get() = safeAsIr<IrSimpleFunction>()?.isOperator ?: false

    override val CallableSymbolMarker.isTailrec: Boolean
        get() = safeAsIr<IrSimpleFunction>()?.isTailrec ?: false

    override val PropertySymbolMarker.isVar: Boolean
        get() = asIr().isVar

    override val PropertySymbolMarker.isLateinit: Boolean
        get() = asIr().isLateinit

    override val PropertySymbolMarker.isConst: Boolean
        get() = asIr().isConst

    override val PropertySymbolMarker.getter: FunctionSymbolMarker?
        get() = asIr().getter?.symbol

    override val PropertySymbolMarker.setter: FunctionSymbolMarker?
        get() = asIr().setter?.symbol

    override val PropertySymbolMarker.contextParameters: List<ValueParameterSymbolMarker>
        get() = asIr()
            .getter
            ?.parameters
            ?.filter { it.kind == IrParameterKind.Context }
            ?.map { it.symbol }
            ?: emptyList()

    override fun createExpectActualTypeParameterSubstitutor(
        expectActualTypeParameters: List<Pair<TypeParameterSymbolMarker, TypeParameterSymbolMarker>>,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): TypeSubstitutorMarker {
        val typeParametersToArguments = expectActualTypeParameters.associate { (expect, actual) ->
            (expect as IrTypeParameterSymbol) to (actual as IrTypeParameterSymbol).owner.defaultType
        }
        val substitutor = IrTypeSubstitutor(typeParametersToArguments, allowEmptySubstitution = true)
        return when (parentSubstitutor) {
            null -> substitutor
            is AbstractIrTypeSubstitutor -> IrChainedSubstitutor(parentSubstitutor, substitutor)
            else -> shouldNotBeCalled()
        }
    }

    /*
     * [isActualDeclaration] flag is needed to correctly determine scope for specific class
     * In IR there are no scopes, all declarations are stored inside IrClass itself, so this flag
     *   has no sense in IR context
     */
    override fun RegularClassSymbolMarker.collectAllMembers(isActualDeclaration: Boolean): List<DeclarationSymbolMarker> {
        return asIr().declarations.filter { it !is IrAnonymousInitializer && !it.isStaticFun() }.map { it.symbol }
    }

    override fun RegularClassSymbolMarker.collectAllStaticCallables(isActualDeclaration: Boolean): List<CallableSymbolMarker> {
        return asIr().declarations.filter { it.isStaticFun() }.mapNotNull { it.symbol as? CallableSymbolMarker }
    }

    override fun RegularClassSymbolMarker.getCallablesForExpectClass(name: Name): List<CallableSymbolMarker> {
        return asIr().declarations.filter { it.getNameWithAssert() == name }.mapNotNull { it.symbol as? CallableSymbolMarker }
    }

    override fun RegularClassSymbolMarker.collectEnumEntryNames(): List<Name> {
        return asIr().declarations.filterIsInstance<IrEnumEntry>().map { it.name }
    }

    override fun RegularClassSymbolMarker.collectEnumEntries(): List<DeclarationSymbolMarker> {
        return asIr().declarations.filterIsInstance<IrEnumEntry>().map { it.symbol }
    }

    override val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
        get() = (asIr().parent as? IrClass)?.defaultType

    override val CallableSymbolMarker.extensionReceiverType: IrType?
        get() = when (this) {
            is IrFunctionSymbol -> owner.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
            is IrPropertySymbol -> owner.getter?.parameters?.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
            else -> null
        }

    override val CallableSymbolMarker.extensionReceiverTypeRef: TypeRefMarker?
        get() = extensionReceiverType

    override val CallableSymbolMarker.returnType: IrType
        get() = processIr(
            onFunction = { it.returnType },
            onProperty = { it.getter?.returnType ?: it.backingField?.type ?: error("No type for property: $it") },
            onField = { it.type },
            onValueParameter = { it.type },
            onEnumEntry = { it.parentAsClass.defaultType }
        )

    override val CallableSymbolMarker.returnTypeRef: TypeRefMarker
        get() = returnType


    override val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = processIr(
            onFunction = { it.typeParameters.map { parameter -> parameter.symbol } },
            onProperty = { it.getter?.symbol?.typeParameters.orEmpty() },
            onField = { emptyList() },
            onValueParameter = { emptyList() },
            onEnumEntry = { emptyList() }
        )

    override fun FunctionSymbolMarker.allRecursivelyOverriddenDeclarationsIncludingSelf(containingClass: RegularClassSymbolMarker?): List<CallableSymbolMarker> =
        when (val node = asIr()) {
            is IrConstructor -> listOf(this)
            is IrSimpleFunction -> (listOf(this) + node.overriddenSymbols)
                // Tests work even if you don't filter out fake-overrides. Filtering fake-overrides is needed because
                // the returned descriptors are compared by `equals`. And `equals` for fake-overrides is weird.
                // I didn't manage to invent a test that would check this condition
                .filter { !it.asIr().isFakeOverride }
        }

    override val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>
        get() = asIr().parameters
            .filter { it.kind == IrParameterKind.Regular }
            .map { it.symbol }

    override val FunctionSymbolMarker.contextParameters: List<ValueParameterSymbolMarker>
        get() = asIr().parameters
            .filter { it.kind == IrParameterKind.Context }
            .map { it.symbol }

    override val ValueParameterSymbolMarker.isVararg: Boolean
        get() = asIr().isVararg
    override val ValueParameterSymbolMarker.isNoinline: Boolean
        get() = asIr().isNoinline
    override val ValueParameterSymbolMarker.isCrossinline: Boolean
        get() = asIr().isCrossinline
    override val ValueParameterSymbolMarker.hasDefaultValue: Boolean
        get() = asIr().hasDefaultValue()

    override val ValueParameterSymbolMarker.hasDefaultValueNonRecursive: Boolean
        get() = asIr().defaultValue != null

    override fun CallableSymbolMarker.isAnnotationConstructor(): Boolean {
        val irConstructor = safeAsIr<IrConstructor>() ?: return false
        return irConstructor.constructedClass.isAnnotationClass
    }

    override val TypeParameterSymbolMarker.bounds: List<IrType>
        get() = asIr().superTypes
    override val TypeParameterSymbolMarker.boundsTypeRefs: List<TypeRefMarker>
        get() = bounds
    override val TypeParameterSymbolMarker.variance: Variance
        get() = asIr().variance
    override val TypeParameterSymbolMarker.isReified: Boolean
        get() = asIr().isReified

    override fun areCompatibleExpectActualTypes(
        expectType: KotlinTypeMarker?,
        actualType: KotlinTypeMarker?,
        parameterOfAnnotationComparisonMode: Boolean,
        dynamicTypesEqualToAnything: Boolean
    ): Boolean {
        if (expectType == null) return actualType == null
        if (actualType == null) return false
        /*
         * Here we need to actualize both types, because of following situation:
         *
         *   // MODULE: common
         *   expect fun foo(): S // (1)
         *   expect class S
         *
         *   // MODULE: intermediate
         *   actual fun foo(): S = null!! // (2)
         *
         *   // MODULE: platform
         *   actual typealias S = String
         *
         * When we match return types of (1) and (2) they both will have original type `S`, but from
         *   perspective of module `platform` it should be replaced with `String`
         */
        val actualizedExpectType = expectType.actualize()
        val actualizedActualType = actualType.actualize()

        if (parameterOfAnnotationComparisonMode && actualizedExpectType is IrSimpleType && actualizedExpectType.isArray() &&
            actualizedActualType is IrSimpleType && actualizedActualType.isArray()
        ) {
            return AbstractTypeChecker.equalTypes(
                createTypeCheckerState(),
                actualizedExpectType.convertToArrayWithOutProjections(),
                actualizedActualType.convertToArrayWithOutProjections()
            )
        }

        return AbstractTypeChecker.equalTypes(
            createTypeCheckerState(),
            actualizedExpectType,
            actualizedActualType
        )
    }

    private fun IrSimpleType.convertToArrayWithOutProjections(): IrSimpleType {
        val argumentsWithOutProjection = List(arguments.size) { i ->
            val typeArgument = arguments[i]
            if (typeArgument !is IrSimpleType) typeArgument
            else makeTypeProjection(typeArgument, Variance.OUT_VARIANCE)
        }
        return IrSimpleTypeImpl(classifier, isNullable(), argumentsWithOutProjection, annotations)
    }

    private fun createTypeCheckerState(): TypeCheckerState {
        return createIrTypeCheckerState(this)
    }

    override fun isSubtypeOf(superType: KotlinTypeMarker, subType: KotlinTypeMarker): Boolean {
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerState(),
            subType = subType.actualize(),
            superType = superType.actualize()
        )
    }

    // TODO(KT-76728): potentially slow place
    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        return with(typeContext) {
            supertypes().map { it.actualize() }
        }
    }

    private fun KotlinTypeMarker.actualize(): IrType {
        return actualizingSubstitutor.substitute(this as IrType)
    }

    private val actualizingSubstitutor = ActualizingSubstitutor()

    private inner class ActualizingSubstitutor : AbstractIrTypeSubstitutor() {
        override fun substitute(type: IrType): IrType {
            return substituteOrNull(type) ?: type
        }

        private fun substituteOrNull(type: IrType): IrType? {
            if (type !is IrSimpleType) return null
            val newClassifier = (type.classifier.owner as? IrClass)?.let { expectToActualClassMap[it.classIdOrFail] }
            val newArguments = ArrayList<IrTypeArgument>(type.arguments.size)
            var argumentsChanged = false
            for (argument in type.arguments) {
                val newArgument = substituteArgumentOrNull(argument)
                if (newArgument != null) {
                    newArguments += newArgument
                    argumentsChanged = true
                } else {
                    newArguments += argument
                }
            }
            if (newClassifier == null && !argumentsChanged) {
                return null
            }
            return IrSimpleTypeImpl(
                classifier = newClassifier ?: type.classifier,
                type.nullability,
                newArguments,
                type.annotations
            )
        }

        private fun substituteArgumentOrNull(argument: IrTypeArgument): IrTypeArgument? {
            return when (argument) {
                is IrStarProjection -> null
                is IrType -> substituteOrNull(argument)
                is IrTypeProjection -> {
                    val newType = substituteOrNull(argument.type) ?: return null
                    makeTypeProjection(newType, argument.variance)
                }
            }
        }
    }

    override fun RegularClassSymbolMarker.isSamInterface(): Boolean =
        this.asIr().functions.singleOrNull { it.modality == Modality.ABSTRACT } != null

    override fun CallableSymbolMarker.isFakeOverride(containingExpectClass: RegularClassSymbolMarker?): Boolean {
        return asIr().isFakeOverride
    }

    override val CallableSymbolMarker.isDelegatedMember: Boolean
        get() = asIr().origin == IrDeclarationOrigin.DELEGATED_MEMBER

    override val CallableSymbolMarker.hasStableParameterNames: Boolean
        get() {
            var ir = asIr()

            if (ir.isFakeOverride && ir is IrOverridableDeclaration<*>) {
                ir.resolveFakeOverrideMaybeAbstract()?.let { ir = it }
            }

            return when (ir.origin) {
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB,
                -> false
                else -> true
            }
        }

    override val CallableSymbolMarker.isJavaField: Boolean
        get() = this is IrPropertySymbol && owner.isPropertyForJavaField()

    override val CallableSymbolMarker.canBeActualizedByJavaField: Boolean
        get() = this is IrPropertySymbol && callableId == abstractMutableListModCountCallableId

    override fun onMatchedMembers(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?,
    ) {
        require(expectSymbol is IrSymbol)
        require(actualSymbol is IrSymbol)
        when (expectSymbol) {
            is IrClassSymbol -> {
                val actualClassSymbol = when (actualSymbol) {
                    is IrClassSymbol -> actualSymbol
                    is IrTypeAliasSymbol -> actualSymbol.owner.expandedType.getClass()!!.symbol
                    else -> actualSymbol.unexpectedSymbolKind<IrClassifierSymbol>()
                }
                onMatchedDeclarations(expectSymbol, actualClassSymbol)
            }
            else -> onMatchedDeclarations(expectSymbol, actualSymbol)
        }
    }

    abstract fun onMatchedDeclarations(expectSymbol: IrSymbol, actualSymbol: IrSymbol)

    override val DeclarationSymbolMarker.annotations: List<AnnotationCallInfo>
        get() = asIr().annotations.map(::AnnotationCallInfoImpl)

    override fun areAnnotationArgumentsEqual(
        expectAnnotation: AnnotationCallInfo, actualAnnotation: AnnotationCallInfo,
        collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
    ): Boolean {
        fun AnnotationCallInfo.getIrElement(): IrConstructorCall = (this as AnnotationCallInfoImpl).irElement

        return areIrExpressionConstValuesEqual(
            expectAnnotation.getIrElement(),
            actualAnnotation.getIrElement(),
            collectionArgumentsCompatibilityCheckStrategy,
        )
    }

    internal fun getClassIdAfterActualization(classId: ClassId): ClassId {
        return expectToActualClassMap[classId]?.classId ?: classId
    }

    private inner class AnnotationCallInfoImpl(val irElement: IrConstructorCall) : AnnotationCallInfo {
        override val annotationSymbol: IrConstructorCall = irElement

        override val classId: ClassId?
            get() = getAnnotationClass()?.classId

        override val isRetentionSource: Boolean
            get() = getAnnotationClass()?.getAnnotationRetention() == KotlinRetention.SOURCE

        override val isOptIn: Boolean
            get() = getAnnotationClass()?.hasAnnotation(OptInNames.REQUIRES_OPT_IN_FQ_NAME) ?: false

        private fun getAnnotationClass(): IrClass? {
            val annotationClass = irElement.type.getClass() ?: return null
            return expectToActualClassMap[annotationClass.classId]?.owner ?: annotationClass
        }
    }

    override val DeclarationSymbolMarker.hasSourceAnnotationsErased: Boolean
        get() {
            val ir = asIr()
            return ir.startOffset < 0 && ir.origin !is IrDeclarationOrigin.GeneratedByPlugin
        }

    // IR checker traverses member scope itself and collects mappings
    override val checkClassScopesForAnnotationCompatibility = false

    /**
     * From IR checker point of view geter and seter are usual methods, so they don't need
     * special handling inside checker.
     * This is to prevent duplicated reports of diagnostic.
     */
    override val checkPropertyAccessorsForAnnotationsCompatibility = false

    /**
     * Same as [checkPropertyAccessorsForAnnotationsCompatibility], enum entries are usual
     * callables for IR checker, so they don't need special handling.
     */
    override val checkEnumEntriesForAnnotationsCompatibility = false

    override fun skipCheckingAnnotationsOfActualClassMember(actualMember: DeclarationSymbolMarker): Boolean = error("Should not be called")

    /**
     * We want to skip checking if the topmost top-level declaration for both symbols came from dependencies
     */
    override fun skipCheckingOnExpectActualPair(
        expectMember: DeclarationSymbolMarker,
        actualMember: DeclarationSymbolMarker,
    ): Boolean {
        val expectContainingClass = expectMember.asIr().topmostParentClassifier()
        val actualContainingClass = actualMember.asIr().topmostParentClassifier()
        return expectContainingClass.origin.isExternal && actualContainingClass.origin.isExternal
    }

    private val IrDeclarationOrigin.isExternal: Boolean
        get() = when (this) {
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB -> true
            else -> false
        }

    private tailrec fun IrDeclaration.topmostParentClassifier(): IrDeclaration {
        val parent = parent
        return when {
            parent is IrClass || parent is IrTypeAlias -> parent.topmostParentClassifier()
            this is IrClass || this is IrTypeAlias -> this
            else -> this
        }
    }

    override fun findPotentialExpectClassMembersForActual(
        expectClass: RegularClassSymbolMarker,
        actualClass: RegularClassSymbolMarker,
        actualMember: DeclarationSymbolMarker,
    ): Map<out DeclarationSymbolMarker, ExpectActualMatchingCompatibility> = error("Should not be called")

    // It's a stub, because not needed anywhere
    private object IrSourceElementStub : SourceElementMarker

    override fun DeclarationSymbolMarker.getSourceElement(): SourceElementMarker = IrSourceElementStub

    override fun TypeRefMarker.getClassId(): ClassId? = (this as IrType).getClass()?.classId

    override fun checkAnnotationsOnTypeRefAndArguments(
        expectContainingSymbol: DeclarationSymbolMarker,
        actualContainingSymbol: DeclarationSymbolMarker,
        expectTypeRef: TypeRefMarker,
        actualTypeRef: TypeRefMarker,
        checker: ExpectActualMatchingContext.AnnotationsCheckerCallback
    ) {
        check(expectTypeRef is IrType && actualTypeRef is IrType)

        fun IrType.getAnnotations() = annotations.map(::AnnotationCallInfoImpl)

        checker.check(expectTypeRef.getAnnotations(), actualTypeRef.getAnnotations(), IrSourceElementStub)

        if (expectTypeRef !is IrSimpleType || actualTypeRef !is IrSimpleType) return
        if (expectTypeRef.arguments.size != actualTypeRef.arguments.size) return

        for ((expectArg, actualArg) in expectTypeRef.arguments.zip(actualTypeRef.arguments)) {
            val expectArgType = expectArg.typeOrNull ?: continue
            val actualArgType = actualArg.typeOrNull ?: continue
            checkAnnotationsOnTypeRefAndArguments(expectContainingSymbol, actualContainingSymbol, expectArgType, actualArgType, checker)
        }
    }
}

private fun IrDeclaration.isStaticFun(): Boolean = this is IrSimpleFunction && isStatic
