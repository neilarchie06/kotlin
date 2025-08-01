/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.backend.common.report
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.util.isExpect
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.correspondingEnumEntry
import org.jetbrains.kotlin.ir.backend.js.getInstanceFun
import org.jetbrains.kotlin.ir.backend.js.initEntryInstancesFun
import org.jetbrains.kotlin.ir.backend.js.lower.ES6_BOX_PARAMETER
import org.jetbrains.kotlin.ir.backend.js.lower.isBoxParameter
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private const val magicPropertyName = "__doNotUseOrImplementIt"

class ExportModelGenerator(val context: JsIrBackendContext, val generateNamespacesForPackages: Boolean) {
    private val transitiveExportCollector = TransitiveExportCollector(context)

    fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.packageFqName
        val exports = file.declarations.memoryOptimizedMapNotNull { declaration ->
            declaration.takeIf { it.couldBeConvertedToExplicitExport() != true }?.let(::exportDeclaration)
        }

        return when {
            exports.isEmpty() -> emptyList()
            !generateNamespacesForPackages || namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        val candidate = getExportCandidate(declaration) ?: return null
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) return null

        return when (candidate) {
            is IrSimpleFunction -> exportFunction(candidate)
            is IrProperty -> exportProperty(candidate)
            is IrClass -> exportClass(candidate)
            is IrField -> null
            else -> irError("Can't export declaration") {
                withIrEntry("candidate", candidate)
            }
        }?.withAttributesFor(candidate)
    }

    private fun exportClass(candidate: IrClass): ExportedDeclaration? {
        val superTypes = candidate.defaultType.collectSuperTransitiveHierarchy() + candidate.superTypes

        return if (candidate.isEnumClass) {
            exportEnumClass(candidate, superTypes)
        } else {
            exportOrdinaryClass(candidate, superTypes)
        }
    }


    private fun exportFunction(function: IrSimpleFunction): ExportedDeclaration? {
        return when (val exportability = functionExportability(function)) {
            is Exportability.NotNeeded, is Exportability.Implicit -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val parent = function.parent
                val realOverrideTarget = function.realOverrideTarget
                ExportedFunction(
                    function.getExportedIdentifier(),
                    returnType = exportType(function.returnType, function),
                    typeParameters = function.typeParameters.memoryOptimizedMap { exportTypeParameter(it, function) },
                    isMember = parent is IrClass,
                    isStatic = function.isStaticMethod,
                    isAbstract = parent is IrClass && !parent.isInterface && function.modality == Modality.ABSTRACT,
                    isProtected = function.visibility == DescriptorVisibilities.PROTECTED,
                    ir = function,
                    parameters = function.nonDispatchParameters
                        .filter { it.shouldBeExported() }
                        .memoryOptimizedMap {
                            exportParameter(
                                it,
                                it.hasDefaultValue || realOverrideTarget.parameters[it.indexInParameters].hasDefaultValue
                            )
                        }
                )
            }
        }
    }

    private fun exportConstructor(constructor: IrConstructor): ExportedDeclaration? {
        if (!constructor.isPrimary) return null
        return ExportedConstructor(
            parameters = constructor.nonDispatchParameters
                .filterNot { it.isBoxParameter }
                .memoryOptimizedMap { exportParameter(it, it.hasDefaultValue) },
            visibility = constructor.visibility.toExportedVisibility()
        )
    }

    private fun exportParameter(parameter: IrValueParameter, hasDefaultValue: Boolean): ExportedParameter {
        // Parameter names do not matter in d.ts files. They can be renamed as we like
        var parameterName = sanitizeName(parameter.name.asString(), withHash = false)
        if (parameterName in allReservedWords)
            parameterName = "_$parameterName"

        return ExportedParameter(
            parameterName,
            exportType(parameter.type, parameter),
            hasDefaultValue
        )
    }

    private val IrValueParameter.hasDefaultValue: Boolean
        get() = origin == JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER

    private fun exportProperty(property: IrProperty): ExportedDeclaration? {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            // Frontend will report an error on an attempt to export an extension property.
            // Just to be safe, filter out such properties here as well.
            if (accessor.parameters.any { it.kind == IrParameterKind.ExtensionReceiver })
                return null
            if (accessor.isFakeOverride && !accessor.isAllowedFakeOverriddenDeclaration(context)) {
                return null
            }
        }

        return exportPropertyUnsafely(property)
    }

    private fun exportPropertyUnsafely(
        property: IrProperty,
        specializeType: ExportedType? = null
    ): ExportedDeclaration {
        val parentClass = property.parent as? IrClass
        val isOptional = property.isEffectivelyExternal() &&
                property.parent is IrClass &&
                property.getter?.returnType?.isNullable() == true

        return ExportedProperty(
            name = property.getExportedIdentifier(),
            type = specializeType ?: exportType(property.getter!!.returnType, property),
            mutable = property.isVar,
            isMember = parentClass != null,
            isAbstract = parentClass?.isInterface == false && property.modality == Modality.ABSTRACT,
            isProtected = property.visibility == DescriptorVisibilities.PROTECTED,
            isField = parentClass?.isInterface == true,
            irGetter = property.getter,
            irSetter = property.setter,
            isOptional = isOptional,
            isStatic = (property.getter ?: property.setter)?.isStaticMethodOfClass == true,
        )
    }

    private fun exportEnumEntry(field: IrField, enumEntries: Map<IrEnumEntry, Int>): ExportedProperty {
        val irEnumEntry = field.correspondingEnumEntry
            ?: irError("Unable to find enum entry") {
                withIrEntry("field", field)
            }

        val parentClass = field.parent as IrClass

        val name = irEnumEntry.getExportedIdentifier()
        val ordinal = enumEntries.getValue(irEnumEntry)

        fun fakeProperty(name: String, type: ExportedType) =
            ExportedProperty(name = name, type = type, mutable = false, isMember = true)

        val nameProperty = fakeProperty(
            name = "name",
            type = ExportedType.LiteralType.StringLiteralType(name),
        )

        val ordinalProperty = fakeProperty(
            name = "ordinal",
            type = ExportedType.LiteralType.NumberLiteralType(ordinal),
        )

        val type = ExportedType.InlineInterfaceType(
            listOf(nameProperty, ordinalProperty)
        )

        return ExportedProperty(
            name = name,
            type = ExportedType.IntersectionType(exportType(parentClass.defaultType), type),
            mutable = false,
            isMember = true,
            isStatic = true,
            isProtected = parentClass.visibility == DescriptorVisibilities.PROTECTED,
            irGetter = irEnumEntry.getInstanceFun
                ?: irError("Unable to find get instance fun") {
                    withIrEntry("field", field)
                },
        )
    }

    private fun classExportability(klass: IrClass): Exportability {
        when (klass.kind) {
            ClassKind.ANNOTATION_CLASS ->
                return Exportability.Prohibited("Class ${klass.fqNameWhenAvailable} with kind: ${klass.kind}")

            ClassKind.OBJECT,
            ClassKind.CLASS,
            ClassKind.INTERFACE,
            ClassKind.ENUM_CLASS,
            ClassKind.ENUM_ENTRY -> {
            }
        }

        if (klass.isJsImplicitExport()) {
            return Exportability.Implicit
        }

        if (klass.isSingleFieldValueClass)
            return Exportability.Prohibited("Inline class ${klass.fqNameWhenAvailable}")

        return Exportability.Allowed
    }

    private fun exportDeclarationImplicitly(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration {
        val typeParameters = klass.typeParameters.memoryOptimizedMap { exportTypeParameter(it, klass) }
        val superInterfaces = superTypes
            .filter { (it.classifierOrFail.owner as? IrDeclaration)?.isExportedImplicitlyOrExplicitly(context) ?: false }
            .map { exportType(it) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()
        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes)
        return ExportedRegularClass(
            name = name,
            isInterface = true,
            isAbstract = false,
            superClasses = emptyList(),
            superInterfaces = superInterfaces,
            typeParameters = typeParameters,
            members = members,
            nestedClasses = nestedClasses,
            ir = klass
        )
    }

    private fun exportOrdinaryClass(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes)
            Exportability.Allowed -> {}
        }

        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes)

        return exportClass(
            klass,
            superTypes,
            members,
            nestedClasses
        )
    }

    private fun exportEnumClass(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes)
            Exportability.Allowed -> {}
        }

        val enumEntries = klass
            .declarations
            .filterIsInstance<IrField>()
            .mapNotNull { it.correspondingEnumEntry }

        val enumEntriesToOrdinal: Map<IrEnumEntry, Int> =
            enumEntries
                .keysToMap(enumEntries::indexOf)

        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes) { candidate ->
            val enumExportedMember = exportAsEnumMember(candidate, enumEntriesToOrdinal)
            enumExportedMember
        }

        val privateConstructor = ExportedConstructor(
            parameters = emptyList(),
            visibility = ExportedVisibility.PRIVATE
        )

        return exportClass(
            klass,
            superTypes,
            listOf(privateConstructor) memoryOptimizedPlus members,
            nestedClasses
        )
    }

    private fun exportClassDeclarations(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        specialProcessing: (IrDeclarationWithName) -> ExportedDeclaration? = { null }
    ): ExportedClassDeclarationsInfo {
        val members = mutableListOf<ExportedDeclaration>()
        val specialMembers = mutableListOf<ExportedDeclaration>()
        val nestedClasses = mutableListOf<ExportedClass>()
        val isImplicitlyExportedClass = klass.isJsImplicitExport()

        for (declaration in klass.declarations) {
            val candidate = getExportCandidate(declaration) ?: continue
            if (isImplicitlyExportedClass && candidate !is IrClass) continue
            if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) continue
            if (candidate.isFakeOverride && klass.isInterface) continue

            val processingResult = specialProcessing(candidate)
            if (processingResult != null) {
                specialMembers.add(processingResult)
                continue
            }

            when (candidate) {
                is IrSimpleFunction ->
                    members.addIfNotNull(exportFunction(candidate)?.withAttributesFor(candidate))

                is IrConstructor ->
                    members.addIfNotNull(exportConstructor(candidate)?.withAttributesFor(candidate))

                is IrProperty ->
                    members.addIfNotNull(exportProperty(candidate)?.withAttributesFor(candidate))

                is IrClass -> {
                    if (klass.isInterface) {
                        nestedClasses.addIfNotNull(klass.companionObject()?.let { exportClass(it) as? ExportedClass }?.withAttributesFor(candidate))
                    } else {
                        val ec = exportClass(candidate)?.withAttributesFor(candidate)
                        if (ec is ExportedClass) {
                            nestedClasses.add(ec)
                        } else {
                            members.addIfNotNull(ec)
                        }
                    }
                }

                is IrField -> {
                    assert(
                        candidate.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE ||
                                candidate.origin == IrDeclarationOrigin.FIELD_FOR_OUTER_THIS ||
                                candidate.correspondingPropertySymbol != null
                    ) {
                        "Unexpected field without property ${candidate.fqNameWhenAvailable}"
                    }
                }

                else -> irError("Can't export member declaration") {
                    withIrEntry("declaration", declaration)
                }
            }
        }

        if (klass.shouldContainImplementationOfMagicProperty(superTypes)) {
            members.addMagicPropertyForInterfaceImplementation(klass, superTypes)
        } else if (klass.shouldNotBeImplemented()) {
            members.addMagicInterfaceProperty(klass)
        }

        return ExportedClassDeclarationsInfo(
            specialMembers + members,
            nestedClasses
        )
    }

    private fun IrClass.shouldNotBeImplemented(): Boolean {
        return isInterface && !isExternal || isJsImplicitExport()
    }

    private fun IrValueParameter.shouldBeExported(): Boolean {
        return origin != JsLoweredDeclarationOrigin.JS_SUPER_CONTEXT_PARAMETER && origin != ES6_BOX_PARAMETER
    }

    private fun IrClass.shouldContainImplementationOfMagicProperty(superTypes: Iterable<IrType>): Boolean {
        return !isExternal && superTypes.any {
            val superClass = it.classOrNull?.owner ?: return@any false
            superClass.isInterface && it.shouldAddMagicPropertyOfSuper() || superClass.isJsImplicitExport()
        }
    }

    private fun MutableList<ExportedDeclaration>.addMagicInterfaceProperty(klass: IrClass) {
        add(ExportedProperty(name = magicPropertyName, type = klass.generateTagType(), mutable = false, isMember = true, isField = true))
    }

    private fun MutableList<ExportedDeclaration>.addMagicPropertyForInterfaceImplementation(klass: IrClass, superTypes: Iterable<IrType>) {
        val allSuperTypesWithMagicProperty = superTypes.filter { it.shouldAddMagicPropertyOfSuper() }

        if (allSuperTypesWithMagicProperty.isEmpty()) {
            return
        }

        val intersectionOfTypes = allSuperTypesWithMagicProperty
            .map { ExportedType.PropertyType(exportType(it), ExportedType.LiteralType.StringLiteralType(magicPropertyName)) }
            .reduce(ExportedType::IntersectionType)
            .let { if (klass.shouldNotBeImplemented()) ExportedType.IntersectionType(klass.generateTagType(), it) else it }

        add(ExportedProperty(name = magicPropertyName, type = intersectionOfTypes, mutable = false, isMember = true, isField = true))
    }

    private fun IrType.shouldAddMagicPropertyOfSuper(): Boolean {
        return classOrNull?.owner?.isOwnMagicPropertyAdded() ?: false
    }

    private fun IrClass.isOwnMagicPropertyAdded(): Boolean {
        if (isJsImplicitExport()) return true
        if (!isExported(context)) return false
        return isInterface && !isExternal || superTypes.any {
            it.classOrNull?.owner?.isOwnMagicPropertyAdded() == true
        }
    }

    private fun IrClass.generateTagType(): ExportedType {
        return ExportedType.InlineInterfaceType(
            listOf(
                ExportedProperty(
                    name = getFqNameWithJsNameWhenAvailable(true).asString(),
                    type = ExportedType.Primitive.UniqueSymbol,
                    mutable = false,
                    isMember = true,
                    isField = true,
                )
            )
        )
    }

    private fun exportClass(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        members: List<ExportedDeclaration>,
        nestedClasses: List<ExportedClass>,
    ): ExportedDeclaration {
        val typeParameters = klass.typeParameters.memoryOptimizedMap { exportTypeParameter(it, klass) }

        val superClasses = superTypes
            .filter { !it.classifierOrFail.isInterface && it.canBeUsedAsSuperTypeOfExportedClasses() }
            .map { exportType(it, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val superInterfaces = superTypes
            .filter { it.shouldPresentInsideImplementsClause() }
            .map { exportType(it, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifierForClass()

        return if (klass.kind == ClassKind.OBJECT) {
            return ExportedObject(
                ir = klass,
                name = name,
                members = members,
                superClasses = superClasses,
                nestedClasses = nestedClasses,
                superInterfaces = superInterfaces,
                irGetter = klass.objectGetInstanceFunction!!
            )
        } else {
            ExportedRegularClass(
                name = name,
                isInterface = klass.isInterface,
                isAbstract = klass.modality == Modality.ABSTRACT || klass.modality == Modality.SEALED || klass.isEnumClass,
                superClasses = superClasses,
                superInterfaces = superInterfaces,
                typeParameters = typeParameters,
                members = members,
                nestedClasses = nestedClasses,
                ir = klass
            )
        }
    }

    private fun IrSimpleType.collectSuperTransitiveHierarchy(): Set<IrType> =
        transitiveExportCollector.collectSuperTypesTransitiveHierarchyFor(this)

    private fun IrType.shouldPresentInsideImplementsClause(): Boolean {
        val classifier = classifierOrFail
        return classifier.isInterface || (classifier.owner as? IrDeclaration)?.isJsImplicitExport() == true
    }

    private fun exportAsEnumMember(
        candidate: IrDeclarationWithName,
        enumEntriesToOrdinal: Map<IrEnumEntry, Int>
    ): ExportedDeclaration? {
        val enumEntries = enumEntriesToOrdinal.keys
        return when (candidate) {
            is IrProperty -> {
                if (candidate.isAllowedFakeOverriddenDeclaration(context)) {
                    val type: ExportedType = when (candidate.getExportedIdentifier()) {
                        "name" -> enumEntries
                            .map { it.getExportedIdentifier() }
                            .map { ExportedType.LiteralType.StringLiteralType(it) }
                            .reduceOrNull { acc: ExportedType, s: ExportedType -> ExportedType.UnionType(acc, s) } ?: return null
                        "ordinal" -> enumEntriesToOrdinal
                            .map { (_, ordinal) -> ExportedType.LiteralType.NumberLiteralType(ordinal) }
                            .reduceOrNull { acc: ExportedType, s: ExportedType -> ExportedType.UnionType(acc, s) } ?: return null
                        else -> return null
                    }
                    exportPropertyUnsafely(
                        candidate,
                        type
                    )
                } else null
            }

            is IrField -> {
                if (candidate.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) {
                    exportEnumEntry(candidate, enumEntriesToOrdinal)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun IrType.canBeUsedAsSuperTypeOfExportedClasses(): Boolean =
        !isAny() &&
                classifierOrNull != context.irBuiltIns.enumClass &&
                (classifierOrNull?.owner as? IrDeclaration)?.isJsImplicitExport() != true

    private fun exportTypeArgument(type: IrTypeArgument, typeOwner: IrDeclaration?): ExportedType {
        if (type is IrTypeProjection)
            return exportType(type.type, typeOwner)

        return ExportedType.ErrorType("UnknownType ${type.render()}")
    }

    fun exportTypeParameter(typeParameter: IrTypeParameter, typeOwner: IrDeclaration?): ExportedType.TypeParameter {
        val constraint = typeParameter.superTypes.asSequence()
            .filter { it != context.irBuiltIns.anyNType }
            .map {
                val exportedType = exportType(it, typeOwner)
                if (exportedType is ExportedType.ImplicitlyExportedType && exportedType.exportedSupertype == ExportedType.Primitive.Any) {
                    exportedType.copy(exportedSupertype = ExportedType.Primitive.Unknown)
                } else {
                    exportedType
                }
            }
            .filter { it !is ExportedType.ErrorType }
            .toList()

        return ExportedType.TypeParameter(
            typeParameter.name.identifier,
            constraint.run {
                when (size) {
                    0 -> null
                    1 -> single()
                    else -> reduce(ExportedType::IntersectionType)
                }
            }
        )
    }

    private val currentlyProcessedTypes = hashSetOf<IrType>()

    private fun exportType(
        type: IrType,
        typeOwner: IrDeclaration? = null,
        shouldCalculateExportedSupertypeForImplicit: Boolean = true,
    ): ExportedType {
        if (type is IrDynamicType || type in currentlyProcessedTypes)
            return ExportedType.Primitive.Any

        if (type !is IrSimpleType)
            return ExportedType.ErrorType("NonSimpleType ${type.render()}")

        currentlyProcessedTypes.add(type)

        val classifier = type.classifier
        val isMarkedNullable = type.isMarkedNullable()
        val nonNullType = type.makeNotNull() as IrSimpleType

        val exportedType = when {
            nonNullType.isBoolean() -> ExportedType.Primitive.Boolean
            nonNullType.isLong() || nonNullType.isULong() -> {
                if (!context.configuration.compileLongAsBigint) {
                    context.report(
                        CompilerMessageSeverity.ERROR,
                        typeOwner,
                        typeOwner?.file,
                        "Long can't be exported without using of the bigint type. Add -Xes-long-as-bigint compiler argument or set target to 'es2015'"
                    )
                }
                ExportedType.Primitive.BigInt
            }
            nonNullType.isPrimitiveType() && !nonNullType.isChar() ->
                ExportedType.Primitive.Number

            nonNullType.isByteArray() -> ExportedType.Primitive.ByteArray
            nonNullType.isShortArray() -> ExportedType.Primitive.ShortArray
            nonNullType.isIntArray() -> ExportedType.Primitive.IntArray
            nonNullType.isFloatArray() -> ExportedType.Primitive.FloatArray
            nonNullType.isDoubleArray() -> ExportedType.Primitive.DoubleArray

            // TODO: Cover these in frontend
            nonNullType.isBooleanArray() -> ExportedType.ErrorType("BooleanArray")
            // TODO: Add BigInt64Array usage (KT-79284)
            nonNullType.isLongArray() -> ExportedType.ErrorType("LongArray")
            nonNullType.isCharArray() -> ExportedType.ErrorType("CharArray")

            nonNullType.isString() -> ExportedType.Primitive.String
            nonNullType.isThrowable() -> ExportedType.Primitive.Throwable
            nonNullType.isAny() -> ExportedType.Primitive.Any  // TODO: Should we wrap Any in a Nullable type?
            nonNullType.isUnit() -> ExportedType.Primitive.Unit
            nonNullType.isNothing() -> ExportedType.Primitive.Nothing
            nonNullType.isArray() -> ExportedType.Array(exportTypeArgument(nonNullType.arguments[0], typeOwner))
            nonNullType.isSuspendFunction() -> ExportedType.ErrorType("Suspend functions are not supported")
            nonNullType.isFunction() -> ExportedType.Function(
                parameterTypes = nonNullType.arguments.dropLast(1).memoryOptimizedMap { exportTypeArgument(it, typeOwner) },
                returnType = exportTypeArgument(nonNullType.arguments.last(), typeOwner)
            )

            classifier is IrTypeParameterSymbol -> ExportedType.TypeParameter(classifier.owner.name.identifier)

            classifier is IrClassSymbol -> {
                val klass = classifier.owner
                val isExported = klass.isExportedImplicitlyOrExplicitly(context)
                val isImplicitlyExported = !isExported && !klass.isExternal
                val isNonExportedExternal = klass.isExternal && !isExported
                val name = klass.getFqNameWithJsNameWhenAvailable(!isNonExportedExternal && generateNamespacesForPackages).asString()

                val exportedSupertype = runIf(shouldCalculateExportedSupertypeForImplicit && isImplicitlyExported) {
                    val transitiveExportedType = nonNullType.collectSuperTransitiveHierarchy()
                    if (transitiveExportedType.isEmpty()) return@runIf null
                    transitiveExportedType
                        .memoryOptimizedMap { exportType(it, typeOwner) }
                        .reduce(ExportedType::IntersectionType)
                } ?: ExportedType.Primitive.Any

                when (klass.kind) {
                    ClassKind.ANNOTATION_CLASS,
                    ClassKind.ENUM_ENTRY ->
                        ExportedType.ErrorType("Class $name with kind: ${klass.kind}")

                    ClassKind.OBJECT ->
                        ExportedType.TypeOf(ExportedType.ClassType(name, emptyList(), klass))

                    ClassKind.CLASS,
                    ClassKind.ENUM_CLASS,
                    ClassKind.INTERFACE -> ExportedType.ClassType(
                        name,
                        type.arguments.memoryOptimizedMap { exportTypeArgument(it, typeOwner) },
                        klass
                    )
                }.withImplicitlyExported(isImplicitlyExported, exportedSupertype)
            }

            else -> irError("Unexpected classifier") {
                withIrEntry("classifier.owner", classifier.owner)
            }
        }

        return exportedType.withNullability(isMarkedNullable)
            .also { currentlyProcessedTypes.remove(type) }
    }

    private fun functionExportability(function: IrSimpleFunction): Exportability {
        if (function.isInline && function.typeParameters.any { it.isReified })
            return Exportability.Prohibited("Inline reified function")
        if (function.isSuspend)
            return Exportability.Prohibited("Suspend function")
        if (function.isFakeOverride && !function.isAllowedFakeOverriddenDeclaration(context))
            return Exportability.NotNeeded
        if (function.origin == JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME ||
            function.origin == JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR ||
            function.origin == JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME ||
            function.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION ||
            function.origin == JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT ||
            function.origin == JsLoweredDeclarationOrigin.ENUM_GET_INSTANCE_FUNCTION
        ) {
            return Exportability.NotNeeded
        }

        val parentClass = function.parent as? IrClass

        if (parentClass != null && parentClass.initEntryInstancesFun == function) {
            return Exportability.NotNeeded
        }

        val nameString = function.name.asString()
        if (nameString.endsWith("-impl"))
            return Exportability.NotNeeded


        // Workaround in case IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER is rewritten.
        // TODO: Remove this check KT-75095
        if (nameString.endsWith("\$") && function.parameters.any { "\$mask" in it.name.asString() }) {
            return Exportability.NotNeeded
        }

        val name = function.getExportedIdentifier()
        // TODO: Use [] syntax instead of prohibiting
        if (parentClass == null && name in allReservedWords)
            return Exportability.Prohibited("Name is a reserved word")

        return Exportability.Allowed
    }
}

sealed class Exportability {
    object Allowed : Exportability()
    object NotNeeded : Exportability()
    object Implicit : Exportability()
    class Prohibited(val reason: String) : Exportability()
}

private class ExportedClassDeclarationsInfo(
    val members: List<ExportedDeclaration>,
    val nestedClasses: List<ExportedClass>
) {
    operator fun component1() = members
    operator fun component2() = nestedClasses
}

private val IrClassifierSymbol.isInterface
    get() = (owner as? IrClass)?.isInterface == true

private val IrFunction.isStaticMethod: Boolean
    get() = isEs6ConstructorReplacement || isStaticMethodOfClass

private fun getExportCandidate(declaration: IrDeclaration): IrDeclarationWithName? {
    // Only actual public declarations with name can be exported
    if (declaration !is IrDeclarationWithVisibility ||
        declaration !is IrDeclarationWithName ||
        !declaration.visibility.isPublicAPI ||
        declaration.isExpect
    ) {
        return null
    }

    // Workaround to get property declarations instead of its lowered accessors.
    if (declaration is IrSimpleFunction) {
        val property = declaration.correspondingPropertySymbol?.owner
        if (property != null) {
            // Return property for getter accessors only to prevent
            // returning it twice (for getter and setter) in the same scope
            return if (property.getter == declaration)
                property
            else
                null
        }
    }

    return declaration
}

private fun shouldDeclarationBeExportedImplicitlyOrExplicitly(
    declaration: IrDeclarationWithName,
    context: JsIrBackendContext,
    source: IrDeclaration = declaration
): Boolean {
    return declaration.isJsImplicitExport() || shouldDeclarationBeExported(declaration, context, source)
}

private fun shouldDeclarationBeExported(
    declaration: IrDeclarationWithName,
    context: JsIrBackendContext,
    source: IrDeclaration = declaration
): Boolean {
    // Formally, user have no ability to annotate EnumEntry as exported, without Enum Class
    // But, when we add @file:JsExport, the annotation appears on the all of enum entries
    // what make a wrong behaviour on non-exported members inside Enum Entry (check exportEnumClass and exportFileWithEnumClass tests)
    if (declaration is IrClass && declaration.kind == ClassKind.ENUM_ENTRY)
        return false

    if (declaration.isJsExportIgnore() || (declaration as? IrDeclarationWithVisibility)?.visibility?.isPublicAPI == false)
        return false

    if (context.additionalExportedDeclarationNames.contains(declaration.fqNameWhenAvailable))
        return true

    if (context.additionalExportedDeclarations.contains(declaration))
        return true

    if (source is IrOverridableDeclaration<*>) {
        val overriddenNonEmpty = source.overriddenSymbols.isNotEmpty()

        if (overriddenNonEmpty) {
            return source.isOverriddenExported(context) ||
                    (source as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions
                    || source.isAllowedFakeOverriddenDeclaration(context)
        }
    }

    if (declaration.isJsExport())
        return true

    return when (val parent = declaration.parent) {
        is IrDeclarationWithName -> shouldDeclarationBeExported(parent, context)
        is IrAnnotationContainer -> parent.isJsExport()
        else -> false
    }
}

fun IrOverridableDeclaration<*>.isAllowedFakeOverriddenDeclaration(context: JsIrBackendContext): Boolean {
    if (isOverriddenEnumProperty(context)) return true

    val firstExportedRealOverride = runIf(isFakeOverride) {
        resolveFakeOverrideMaybeAbstract { it === this || it.isFakeOverride || it.parentClassOrNull?.isExported(context) != true }
    } ?: return false

    return firstExportedRealOverride.parentClassOrNull.isExportedInterface(context) && !firstExportedRealOverride.isJsExportIgnore()
}

fun IrOverridableDeclaration<*>.isOverriddenEnumProperty(context: JsIrBackendContext) =
    overriddenSymbols
        .map { it.owner }
        .filterIsInstanceAnd<IrOverridableDeclaration<*>> {
            it.overriddenSymbols.isEmpty() && it.parentClassOrNull?.symbol == context.irBuiltIns.enumClass
        }
        .isNotEmpty()

fun IrOverridableDeclaration<*>.isOverriddenExported(context: JsIrBackendContext): Boolean =
    overriddenSymbols
        .any {
            val owner = it.owner as IrDeclarationWithName
            val candidate = getExportCandidate(owner) ?: owner
            shouldDeclarationBeExported(candidate, context, owner)
        }

fun IrDeclaration.isExported(context: JsIrBackendContext): Boolean {
    val candidate = getExportCandidate(this) ?: return false
    return shouldDeclarationBeExported(candidate, context, this)
}

fun IrDeclaration.isExportedImplicitlyOrExplicitly(context: JsIrBackendContext): Boolean {
    val candidate = getExportCandidate(this) ?: return false
    return shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, this)
}

fun DescriptorVisibility.toExportedVisibility() =
    when (this) {
        DescriptorVisibilities.PROTECTED -> ExportedVisibility.PROTECTED
        else -> ExportedVisibility.DEFAULT
    }

private val reservedWords = setOf(
    "break",
    "case",
    "catch",
    "class",
    "const",
    "continue",
    "debugger",
    "default",
    "delete",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "finally",
    "for",
    "function",
    "if",
    "import",
    "in",
    "instanceof",
    "new",
    "null",
    "return",
    "super",
    "switch",
    "this",
    "throw",
    "true",
    "try",
    "typeof",
    "var",
    "void",
    "while",
    "with"
)

val strictModeReservedWords = setOf(
    "as",
    "implements",
    "interface",
    "let",
    "package",
    "private",
    "protected",
    "public",
    "static",
    "yield"
)

private val allReservedWords = reservedWords + strictModeReservedWords

fun <T : ExportedDeclaration> T.withAttributesFor(declaration: IrDeclaration): T {
    declaration.getDeprecated()?.let { attributes.add(ExportedAttribute.DeprecatedAttribute(it)) }

    return this
}

fun IrClass.getExportedIdentifierForClass(): String {
    val parentClass = parentClassOrNull
    return if (parentClass != null && isCompanion && parentClass.isInterface) {
        parentClass.getExportedIdentifierForClass()
    } else getExportedIdentifier()
}

fun IrDeclarationWithName.getExportedIdentifier(): String =
    with(getJsNameOrKotlinName()) {
        if (isSpecial)
            irError("Cannot export special name: ${name.asString()} for declaration") {
                withIrEntry("this", this@getExportedIdentifier)
            }
        else identifier
    }

