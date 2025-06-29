/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.mapping

import org.jetbrains.kotlin.backend.common.defaultArgumentsOriginalFunction
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.sanitizeNameIfNeeded
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.extractTypeMappingModeFromAnnotation
import org.jetbrains.kotlin.codegen.state.isMethodWithDeclarationSiteWildcardsFqName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClassBase
import org.jetbrains.kotlin.ir.descriptors.IrBasedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class MethodSignatureMapper(private val context: JvmBackendContext, private val typeMapper: IrTypeMapper) {
    private val typeSystem: IrTypeSystemContext = typeMapper.typeSystem

    fun mapAsmMethod(function: IrFunction): Method =
        mapSignatureSkipGeneric(function).asmMethod

    fun mapFieldSignature(field: IrField): String? {
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
        if (field.correspondingPropertySymbol?.owner?.isVar == true) {
            writeParameterType(sw, field.type, field)
        } else {
            mapReturnType(field, field.type, sw)
        }
        return sw.makeJavaGenericSignature()
    }

    fun mapFunctionName(function: IrFunction, skipSpecial: Boolean = false): String {
        if (function !is IrSimpleFunction) return function.name.asString()

        if (!skipSpecial) {
            if (function.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) {
                val platformName = function.getJvmNameFromAnnotation()
                if (platformName != null) return platformName
            }

            val nameForSpecialFunction = getJvmMethodNameIfSpecial(function)
            if (nameForSpecialFunction != null) return nameForSpecialFunction
        }

        val property = function.correspondingPropertySymbol?.owner
        if (property != null) {
            val propertyName = property.name.asString()
            val propertyParent = property.parentAsClass

            if (propertyParent.isAnnotationClass) return propertyName

            for (i in propertyParent.superTypes.indices) {
                if (propertyParent.superTypes[i].isJavaLangRecord()) return propertyName
            }

            // The enum property getters <get-name> and <get-ordinal> have special names which also
            // apply to their fake overrides. Unfortunately, getJvmMethodNameIfSpecial does not handle
            // fake overrides, so we need a special case here.
            if ((propertyParent.isEnumClass || propertyParent.isEnumEntry) && (propertyName == "name" || propertyName == "ordinal"))
                return propertyName

            if (function.name.isSpecial) {
                val accessorName = if (function.isGetter) JvmAbi.getterName(propertyName) else JvmAbi.setterName(propertyName)
                return mangleMemberNameIfRequired(accessorName, function)
            }
        }

        return mangleMemberNameIfRequired(function.name.asString(), function)
    }

    private fun IrType.isJavaLangRecord(): Boolean =
        getClass()?.hasEqualFqName(JAVA_LANG_RECORD_FQ_NAME) == true

    private fun mangleMemberNameIfRequired(name: String, function: IrSimpleFunction): String {
        val newName = sanitizeNameIfNeeded(name, context.config.languageVersionSettings)

        val suffix = if (function.isTopLevel) {
            if (function.isInvisibleInMultifilePart()) function.parentAsClass.name.asString() else null
        } else {
            function.getInternalFunctionForManglingIfNeeded()?.let {
                NameUtils.sanitizeAsJavaIdentifier(getModuleName(it))
            }
        } ?: return newName

        if (function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            assert(newName.endsWith(JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX)) { "Default adapter should end with \$default: ${function.render()}" }
            return newName.substringBeforeLast(JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX) + "$" + suffix + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX
        }

        return "$newName$$suffix"
    }

    private fun IrSimpleFunction.isInvisibleInMultifilePart(): Boolean =
        name.asString() != "<clinit>" &&
                (parent as? IrClass)?.multifileFacadeForPart != null &&
                (DescriptorVisibilities.isPrivate(suspendFunctionOriginal().visibility) ||
                        originalForDefaultAdapter?.isInvisibleInMultifilePart() == true)

    private fun IrSimpleFunction.getInternalFunctionForManglingIfNeeded(): IrSimpleFunction? {
        if (visibility == DescriptorVisibilities.INTERNAL &&
            origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR &&
            origin != JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR &&
            origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS &&
            origin != IrDeclarationOrigin.PROPERTY_DELEGATE &&
            origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER &&
            !isPublishedApi()
        ) {
            return (originalFunction.takeIf { it != this } as? IrSimpleFunction)
                ?.getInternalFunctionForManglingIfNeeded()
                ?: this
        }
        originalForDefaultAdapter?.getInternalFunctionForManglingIfNeeded()?.let { return it }
        return null
    }

    private val IrSimpleFunction.originalForDefaultAdapter: IrSimpleFunction?
        get() = if (origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            defaultArgumentsOriginalFunction as IrSimpleFunction
        } else null

    private fun getModuleName(function: IrSimpleFunction): String =
        (if (function is IrLazyFunctionBase)
            getJvmModuleNameForDeserialized(function)
        else null) ?: context.state.moduleName

    fun mapReturnType(declaration: IrDeclaration, sw: JvmSignatureWriter? = null, materialized: Boolean = true): Type {
        if (declaration !is IrFunction) {
            require(declaration is IrField) { "Unsupported declaration: $declaration" }
            return mapReturnType(declaration, declaration.type, sw, materialized)
        }

        return when {
            hasVoidReturnType(declaration) -> {
                sw?.writeAsmType(Type.VOID_TYPE)
                Type.VOID_TYPE
            }
            forceBoxedReturnType(declaration) -> {
                typeMapper.mapType(declaration.returnType, TypeMappingMode.RETURN_TYPE_BOXED, sw, materialized)
            }
            else -> mapReturnType(declaration, declaration.returnType, sw, materialized)
        }
    }

    private fun mapReturnType(declaration: IrDeclaration, returnType: IrType, sw: JvmSignatureWriter?, materialized: Boolean = true): Type {
        val isAnnotationMethod = declaration.parent.let { it is IrClass && it.isAnnotationClass }
        if (sw == null || sw.skipGenericSignature()) {
            return typeMapper.mapType(returnType, TypeMappingMode.getModeForReturnTypeNoGeneric(isAnnotationMethod), sw, materialized)
        }

        val mode = getTypeMappingModeForReturnType(typeSystem, declaration, returnType)
        return typeMapper.mapType(returnType, mode, sw, materialized)
    }

    private fun hasVoidReturnType(function: IrFunction): Boolean =
        function is IrConstructor || (function.returnType.isUnit() && !function.isGetter)

    // See also: KotlinTypeMapper.forceBoxedReturnType
    private fun forceBoxedReturnType(function: IrFunction): Boolean =
        (function.hasAnnotation(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME) && function.returnType.isInlineClassType()) ||
                isBoxMethodForInlineClass(function) ||
                forceFoxedReturnTypeOnOverride(function) ||
                forceBoxedReturnTypeOnDefaultImplFun(function) ||
                function.isFromJava() && function.returnType.isInlineClassType()

    private fun forceFoxedReturnTypeOnOverride(function: IrFunction) =
        function is IrSimpleFunction &&
                function.returnType.isPrimitiveType() &&
                function.allOverridden().any { !it.returnType.isPrimitiveType() }

    private fun forceBoxedReturnTypeOnDefaultImplFun(function: IrFunction): Boolean {
        if (function !is IrSimpleFunction) return false
        val originalFun = function.originalFunctionForDefaultImpl ?: return false
        return forceFoxedReturnTypeOnOverride(originalFun)
    }

    private fun isBoxMethodForInlineClass(function: IrFunction): Boolean =
        function.parent.let { it is IrClass && it.isSingleFieldValueClass } &&
                function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER &&
                function.name.asString() == "box-impl"

    fun mapFakeOverrideSignatureSkipGeneric(function: IrFunction): JvmMethodSignature =
        mapSignature(function, skipGenericSignature = true, materialized = false)

    fun mapSignatureSkipGeneric(function: IrFunction): JvmMethodSignature =
        mapSignature(function, true)

    fun mapSignatureWithGeneric(function: IrFunction): JvmMethodGenericSignature =
        mapSignature(function, false)

    private fun mapSignature(
        function: IrFunction,
        skipGenericSignature: Boolean,
        skipSpecial: Boolean = false,
        materialized: Boolean = true
    ): JvmMethodGenericSignature {
        if (function is IrLazyFunctionBase &&
            (!function.isFakeOverride || function.parentAsClass.isFromJava()) &&
            function.initialSignatureFunction != null
        ) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if ((function as? IrSimpleFunction)?.getDifferentNameForJvmBuiltinFunction() == null ||
                (function.parent as? IrClass)?.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            ) {
                return mapSignature(function.initialSignatureFunction!!, skipGenericSignature)
            }
        }

        val sw = if (skipGenericSignature) JvmSignatureWriter() else BothSignatureWriter(BothSignatureWriter.Mode.METHOD)

        typeMapper.writeFormalTypeParameters(function.typeParameters, sw)

        sw.writeParametersStart()

        for (parameter in function.nonDispatchParameters) {
            val type =
                if (parameter.kind == IrParameterKind.Regular && shouldBoxSingleValueParameterForSpecialCaseOfRemove(function))
                    parameter.type.makeNullable()
                else parameter.type
            writeParameter(
                sw = sw,
                isSkippedInGenericSignature = parameter.kind == IrParameterKind.Regular && parameter.isSkippedInGenericSignature,
                type = type,
                function = function,
                materialized = parameter.kind == IrParameterKind.Regular && materialized
            )
        }

        sw.writeReturnType()
        mapReturnType(function, sw, materialized)
        sw.writeReturnTypeEnd()

        val signature = sw.makeJvmMethodSignature(mapFunctionName(function, skipSpecial))

        if (!skipGenericSignature && function is IrSimpleFunction) {
            return GenericSignatureMapper.mapSignature(function, signature)
        }
        return signature
    }

    companion object {
        // Boxing is only necessary for 'remove(E): Boolean' of a MutableCollection<Int> implementation.
        // Otherwise this method might clash with 'remove(I): E' defined in the java.util.List JDK interface (mapped to kotlin 'removeAt').
        fun shouldBoxSingleValueParameterForSpecialCaseOfRemove(irFunction: IrFunction): Boolean {
            if (irFunction !is IrSimpleFunction) return false
            if (irFunction.name.asString() != "remove" && !irFunction.name.asString().startsWith("remove-")) return false
            if (irFunction.isFromJava()) return false
            if (irFunction.parameters.size != 2) return false
            val valueParameterType = irFunction.parameters[1].type
            if (!valueParameterType.unboxInlineClass().isInt()) return false
            return irFunction.allOverridden(false).any { it.parent.kotlinFqName == StandardNames.FqNames.mutableCollection }
        }

        fun getTypeMappingModeForReturnType(
            typeSystem: IrTypeSystemContext, declaration: IrDeclaration, returnType: IrType
        ): TypeMappingMode = with(typeSystem) {
            val isAnnotationMethod = declaration.parent.let { it is IrClass && it.isAnnotationClass }
            extractTypeMappingModeFromAnnotation(
                declaration.suppressWildcardsMode(), returnType, isAnnotationMethod, mapTypeAliases = false
            ) ?: getOptimalModeForReturnType(returnType, isAnnotationMethod)
        }

        fun getTypeMappingModeForParameter(
            typeSystem: IrTypeSystemContext, declaration: IrDeclaration, type: IrType
        ): TypeMappingMode = with(typeSystem) {
            extractTypeMappingModeFromAnnotation(
                declaration.suppressWildcardsMode(), type, isForAnnotationParameter = false, mapTypeAliases = false
            )
                ?: if (declaration.isMethodWithDeclarationSiteWildcards && !declaration.isStaticInlineClassReplacement && type.argumentsCount() != 0) {
                    TypeMappingMode.GENERIC_ARGUMENT // Render all wildcards
                } else {
                    getOptimalModeForValueParameter(type)
                }
        }

        private val IrDeclaration.isMethodWithDeclarationSiteWildcards: Boolean
            get() = this is IrSimpleFunction && allOverridden().any {
                it.fqNameWhenAvailable.isMethodWithDeclarationSiteWildcardsFqName
            }

        private fun IrDeclaration.suppressWildcardsMode(): Boolean? =
            parentsWithSelf.mapNotNull { declaration ->
                when (declaration) {
                    is IrField -> {
                        // Annotations on properties (JvmSuppressWildcards has PROPERTY, not FIELD, in its targets) have been moved
                        // to the synthetic "$annotations" method, but the copy can still be found via the property symbol.
                        declaration.correspondingPropertySymbol?.owner?.getSuppressWildcardsAnnotationValue()
                    }
                    is IrAnnotationContainer -> declaration.getSuppressWildcardsAnnotationValue()
                    else -> null
                }
            }.firstOrNull()

        private fun IrAnnotationContainer.getSuppressWildcardsAnnotationValue(): Boolean? =
            getAnnotation(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME)?.run {
                if (arguments.size >= 1) (arguments[0] as? IrConst)?.value as? Boolean ?: true else null
            }
    }

    private fun writeParameter(
        sw: JvmSignatureWriter,
        isSkippedInGenericSignature: Boolean,
        type: IrType,
        function: IrFunction,
        materialized: Boolean = true
    ) {
        sw.writeParameterType(JvmMethodParameterKind.VALUE, isSkippedInGenericSignature)
        writeParameterType(sw, type, function, materialized)
        sw.writeParameterTypeEnd()
    }

    private fun writeParameterType(sw: JvmSignatureWriter, type: IrType, declaration: IrDeclaration, materialized: Boolean = true) {
        if (sw.skipGenericSignature()) {
            if (type.isInlineClassType() && declaration.isFromJava()) {
                typeMapper.mapType(type, TypeMappingMode.GENERIC_ARGUMENT, sw, materialized)
            } else {
                typeMapper.mapType(type, declaration.wrapInlineClassForExposeFunction(TypeMappingMode.DEFAULT, type), sw, materialized)
            }
            return
        }

        val mode = getTypeMappingModeForParameter(typeSystem, declaration, type)
        typeMapper.mapType(type, declaration.wrapInlineClassForExposeFunction(mode, type), sw, materialized)
    }

    private fun IrDeclaration.wrapInlineClassForExposeFunction(mode: TypeMappingMode, type: IrType): TypeMappingMode {
        if (hasAnnotation(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME) && type.isInlineClassType()) {
            return mode.wrapInlineClassesMode()
        }
        return mode
    }

    // TODO get rid of 'caller' argument
    fun mapToCallableMethod(expression: IrCall, caller: IrFunction?): IrCallableMethod {
        val callee = expression.symbol.owner
        val calleeParent = expression.superQualifierSymbol?.owner ?: callee.parentAsClass
        val owner = typeMapper.mapOwner(calleeParent)

        val isInterface = calleeParent.isJvmInterface
        val isSuperCall = expression.superQualifierSymbol != null

        val invokeOpcode = when {
            callee.dispatchReceiverParameter == null -> Opcodes.INVOKESTATIC
            isSuperCall -> Opcodes.INVOKESPECIAL
            isInterface && !DescriptorVisibilities.isPrivate(callee.visibility) -> Opcodes.INVOKEINTERFACE
            DescriptorVisibilities.isPrivate(callee.visibility) -> Opcodes.INVOKESPECIAL
            else -> Opcodes.INVOKEVIRTUAL
        }

        val declaration =
            if (isSuperCall) resolveSuperCallOfFakeOverride(callee) else findSuperDeclaration(callee)
        val signature =
            if (caller != null && caller.isBridge()) {
                // Do not remap special builtin methods when called from a bridge. The bridges are there to provide the
                // remapped name or signature and forward to the actually declared method.
                mapSignatureSkipGeneric(declaration)
            } else {
                mapOverriddenSpecialBuiltinIfNeeded(declaration, isSuperCall)
                    ?: mapSignatureSkipGeneric(declaration)
            }

        return IrCallableMethod(owner, invokeOpcode, signature, isInterface, declaration.returnType)
    }

    // TODO: get rid of this (probably via some special lowering)
    private fun mapOverriddenSpecialBuiltinIfNeeded(callee: IrFunction, superCall: Boolean): JvmMethodSignature? {
        // Do not remap calls to static replacements of inline class methods, since they have completely different signatures.
        if (callee.isStaticValueClassReplacement) return null
        val overriddenSpecialBuiltinFunction =
            (callee.toIrBasedDescriptor().getOverriddenBuiltinReflectingJvmDescriptor() as IrBasedSimpleFunctionDescriptor?)?.owner
        if (overriddenSpecialBuiltinFunction != null && !superCall) {
            return mapSignatureSkipGeneric(overriddenSpecialBuiltinFunction)
        }

        return null
    }

    // We need to resolve fake override, i.e. find the actual implementation that will be called, because super call should always invoke
    // the most specific (in terms of generics & covariant return type override) signature, and the implementation is guaranteed to have it.
    // Additional complexity comes from the fact that we generate additional methods in classes which inherit from interfaces. Since we
    // don't run lowering phases on IR from dependencies, we cannot just look up the IR to find if the method is going to be there in the
    // JVM class file, we need to reinterpret the fake override instead, in the same way that we're doing during lowering the source IR
    // (see `ClassFakeOverrideReplacement`).
    private fun resolveSuperCallOfFakeOverride(function: IrSimpleFunction): IrSimpleFunction {
        fun shouldMemberBeSkipped(f: IrSimpleFunction): Boolean {
            if (f.isFakeOverride) return context.cachedDeclarations.getClassFakeOverrideReplacement(f) is ClassFakeOverrideReplacement.None
            if (!f.parentAsClass.isInterface) return false
            if (f.modality == Modality.ABSTRACT) return true
            return f.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && !f.isCompiledToJvmDefault(context.config.jvmDefaultMode)
        }

        return function.resolveFakeOverride(::shouldMemberBeSkipped) ?: function
    }

    private fun getJvmMethodNameIfSpecial(irFunction: IrSimpleFunction): String? {
        if (
            irFunction.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT ||
            irFunction.origin == JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT
        ) {
            return null
        }

        return irFunction.getBuiltinSpecialPropertyGetterName()
            ?: irFunction.getDifferentNameForJvmBuiltinFunction()
    }

    private val IrSimpleFunction.isBuiltIn: Boolean
        get() = getPackageFragment().packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME ||
                (parent as? IrClass)?.fqNameWhenAvailable?.toUnsafe()?.let(JavaToKotlinClassMap::mapKotlinToJava) != null

    // From BuiltinMethodsWithDifferentJvmName.isBuiltinFunctionWithDifferentNameInJvm, BuiltinMethodsWithDifferentJvmName.getJvmName
    private fun IrSimpleFunction.getDifferentNameForJvmBuiltinFunction(): String? {
        if (name !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES) return null
        if (!isBuiltIn) return null
        return allOverridden(includeSelf = true)
            .filter { it.isBuiltIn }
            .firstNotNullOfOrNull {
                val signature = it.computeJvmSignature()
                SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME[signature]?.asString()
            }
    }

    private fun IrSimpleFunction.getBuiltinSpecialPropertyGetterName(): String? {
        val propertyName = correspondingPropertySymbol?.owner?.name ?: return null
        if (propertyName !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null
        if (!isBuiltIn) return null
        return allOverridden(includeSelf = true)
            .mapNotNull {
                val property = it.correspondingPropertySymbol!!.owner
                BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[property.fqNameWhenAvailable]?.asString()
            }
            .firstOrNull()
    }

    private fun IrFunction.computeJvmSignature(): String = signatures {
        val classPart = typeMapper.mapType(parentAsClass.defaultType).internalName
        val signature = mapSignature(this@computeJvmSignature, skipGenericSignature = false, skipSpecial = true).toString()
        return signature(classPart, signature)
    }

    // From org.jetbrains.kotlin.load.kotlin.getJvmModuleNameForDeserializedDescriptor
    private fun getJvmModuleNameForDeserialized(function: IrLazyFunctionBase): String? {
        var current: IrDeclarationParent? = function.parent
        while (current != null) {
            when (current) {
                is IrLazyClassBase -> {
                    val moduleName = if (current.isK2) current.moduleName else current.irLazyClassModuleName
                    return moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
                }
                is IrExternalPackageFragment -> {
                    val source = current.containerSource ?: return null
                    return (source as? JvmPackagePartSource)?.moduleName
                }
                else -> current = (current as? IrDeclaration)?.parent ?: return null
            }
        }
        return null
    }

    fun mapToMethodHandle(irFun: IrFunction): Handle {
        val irNonFakeFun = when (irFun) {
            is IrConstructor -> irFun
            is IrSimpleFunction -> findSuperDeclaration(irFun)
        }
        val irParentClass = irNonFakeFun.parent as? IrClass
            ?: throw AssertionError("Unexpected parent: ${irNonFakeFun.parent.render()}")
        val owner = typeMapper.mapOwner(irParentClass)
        val asmMethod = mapAsmMethod(irNonFakeFun)
        val handleTag = getMethodKindTag(irNonFakeFun)
        return Handle(handleTag, owner.internalName, asmMethod.name, asmMethod.descriptor, irParentClass.isJvmInterface)
    }

    private fun getMethodKindTag(irFun: IrFunction): Int =
        when {
            irFun is IrConstructor ->
                Opcodes.H_NEWINVOKESPECIAL
            irFun.dispatchReceiverParameter == null ->
                Opcodes.H_INVOKESTATIC
            irFun.parentAsClass.isJvmInterface ->
                Opcodes.H_INVOKEINTERFACE
            else ->
                Opcodes.H_INVOKEVIRTUAL
        }
}
