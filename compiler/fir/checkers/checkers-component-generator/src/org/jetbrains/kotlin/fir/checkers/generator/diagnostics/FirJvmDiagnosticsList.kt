/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.*
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.PositioningStrategy
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.PrivateForInline

@Suppress("ClassName", "unused")
@OptIn(PrivateForInline::class)
object JVM_DIAGNOSTICS_LIST : DiagnosticList("FirJvmErrors") {
    val DECLARATIONS by object : DiagnosticGroup("Declarations") {
        val OVERRIDE_CANNOT_BE_STATIC by error<PsiElement>()
        val JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION by error<PsiElement>()
        val JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION by error<PsiElement>()
        val JVM_STATIC_ON_NON_PUBLIC_MEMBER by error<PsiElement>()
        val JVM_STATIC_ON_CONST_OR_JVM_FIELD by error<PsiElement>()
        val JVM_STATIC_ON_EXTERNAL_IN_INTERFACE by error<PsiElement>()

        val INAPPLICABLE_JVM_NAME by error<PsiElement>() {
            isSuppressible = true
        }
        val ILLEGAL_JVM_NAME by error<PsiElement>()

        val FUNCTION_DELEGATE_MEMBER_NAME_CLASH by error<PsiElement>(PositioningStrategy.DECLARATION_NAME)

        val VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION by error<PsiElement>()
        val JVM_INLINE_WITHOUT_VALUE_CLASS by error<PsiElement>()

        val INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME by error<PsiElement>()
        val USELESS_JVM_EXPOSE_BOXED by warning<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND by error<PsiElement>()
        val JVM_EXPOSE_BOXED_REQUIRES_NAME by error<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME by error<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME by error<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT by error<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC by error<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS by error<PsiElement>()
        val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED by error<PsiElement>()

        val WRONG_NULLABILITY_FOR_JAVA_OVERRIDE by warning<PsiElement>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("override")
            parameter<FirCallableSymbol<*>>("base")
        }

        val ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE by error<KtNamedFunction>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirNamedFunctionSymbol>("hidden")
            parameter<String>("overrideDescription")
            parameter<FirNamedFunctionSymbol>("regular")
        }

        val IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE by deprecationError<KtTypeReference>(
            ForbidImplementationByDelegationWithDifferentGenericSignature
        ) {
            parameter<FirNamedFunctionSymbol>("base")
            parameter<FirNamedFunctionSymbol>("override")
        }

        val NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION by error<KtDeclaration>(PositioningStrategy.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)

        val PROPERTY_HIDES_JAVA_FIELD by warning<KtCallableDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirFieldSymbol>("hidden")
        }
    }

    val TYPES by object : DiagnosticGroup("Types") {
        val JAVA_TYPE_MISMATCH by error<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<ConeKotlinType>("expectedType")
            parameter<String>("messageSuffix")
        }
        val NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<ConeKotlinType>("expectedType")
            parameter<String>("messageSuffix")
        }
        val NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<ConeKotlinType>("expectedType")
            parameter<String>("messageSuffix")
        }

        val TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<ConeKotlinType>("expectedType")
        }

        val JAVA_CLASS_ON_COMPANION by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<ConeKotlinType>("actualType")
            parameter<ConeKotlinType>("expectedType")
        }
    }

    val TYPE_PARAMETERS by object : DiagnosticGroup("Type parameters") {
        val UPPER_BOUND_CANNOT_BE_ARRAY by error<PsiElement>()
        val UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
        }
        val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
        }
    }

    val ANNOTATIONS by object : DiagnosticGroup("annotations") {
        val STRICTFP_ON_CLASS by error<KtAnnotationEntry>()
        val SYNCHRONIZED_ON_ABSTRACT by error<KtAnnotationEntry>()
        val SYNCHRONIZED_IN_INTERFACE by error<KtAnnotationEntry>()
        val SYNCHRONIZED_IN_ANNOTATION by deprecationError<KtAnnotationEntry>(ForbidJvmAnnotationsOnAnnotationParameters)
        val SYNCHRONIZED_ON_INLINE by warning<KtAnnotationEntry>()
        val SYNCHRONIZED_ON_VALUE_CLASS by deprecationError<KtAnnotationEntry>(ProhibitSynchronizationByValueClassesAndPrimitives)
        val SYNCHRONIZED_ON_SUSPEND by deprecationError<KtAnnotationEntry>(SynchronizedSuspendError)
        val OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS by warning<KtAnnotationEntry>()
        val OVERLOADS_ABSTRACT by error<KtAnnotationEntry>()
        val OVERLOADS_INTERFACE by error<KtAnnotationEntry>()
        val OVERLOADS_LOCAL by error<KtAnnotationEntry>()
        val OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_ERROR by error<KtAnnotationEntry>()
        val OVERLOADS_PRIVATE by warning<KtAnnotationEntry>()
        val DEPRECATED_JAVA_ANNOTATION by warning<KtAnnotationEntry> {
            parameter<FqName>("kotlinName")
        }

        val JVM_PACKAGE_NAME_CANNOT_BE_EMPTY by error<KtAnnotationEntry>()
        val JVM_PACKAGE_NAME_MUST_BE_VALID_NAME by error<KtAnnotationEntry>()
        val JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES by error<KtAnnotationEntry>()

        val POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION by error<KtExpression>()

        val REDUNDANT_REPEATABLE_ANNOTATION by warning<KtAnnotationEntry> {
            parameter<FqName>("kotlinRepeatable")
            parameter<FqName>("javaRepeatable")
        }
        val THROWS_IN_ANNOTATION by deprecationError<KtAnnotationEntry>(ForbidJvmAnnotationsOnAnnotationParameters)
        val JVM_SERIALIZABLE_LAMBDA_ON_INLINED_FUNCTION_LITERALS by deprecationError<KtAnnotationEntry>(
            featureForError = ForbidJvmSerializableLambdaOnInlinedFunctionLiterals
        )
        val INCOMPATIBLE_ANNOTATION_TARGETS by warning<KtAnnotationEntry> {
            parameter<Collection<String>>("missingJavaTargets")
            parameter<Collection<String>>("correspondingKotlinTargets")
        }
        val ANNOTATION_TARGETS_ONLY_IN_JAVA by warning<KtAnnotationEntry>()
    }

    val SUPER by object : DiagnosticGroup("Super") {
        val INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED)
        val JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS by error<KtElement> {
            parameter<ClassId>("javaClassId")
            parameter<ConeKotlinType>("privateKotlinType")
        }
    }

    val RECORDS by object : DiagnosticGroup("JVM Records") {
        val LOCAL_JVM_RECORD by error<PsiElement>()
        val NON_FINAL_JVM_RECORD by error<PsiElement>(PositioningStrategy.NON_FINAL_MODIFIER_OR_NAME)
        val ENUM_JVM_RECORD by error<PsiElement>(PositioningStrategy.ENUM_MODIFIER)
        val JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS by error<PsiElement>()
        val NON_DATA_CLASS_JVM_RECORD by error<PsiElement>()
        val JVM_RECORD_NOT_VAL_PARAMETER by error<PsiElement>()
        val JVM_RECORD_NOT_LAST_VARARG_PARAMETER by error<PsiElement>()
        val INNER_JVM_RECORD by error<PsiElement>(PositioningStrategy.INNER_MODIFIER)
        val FIELD_IN_JVM_RECORD by error<PsiElement>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val DELEGATION_BY_IN_JVM_RECORD by error<PsiElement>()
        val JVM_RECORD_EXTENDS_CLASS by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<ConeKotlinType>("superType")
        }
        val ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE by error<PsiElement>()
    }

    val MODULES by object : DiagnosticGroup("JVM Modules") {
        val JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE by error<PsiElement> {
            parameter<String>("moduleName")
        }
        val JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE by error<PsiElement>()
        val JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE by error<PsiElement>() {
            parameter<String>("moduleName")
            parameter<String>("packageName")
            isSuppressible = true
        }
    }

    val JVM_DEFAULT by object : DiagnosticGroup("JVM Default") {
        val JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val JVM_DEFAULT_WITH_COMPATIBILITY_NOT_IN_NO_COMPATIBILITY_MODE by error<KtElement>()
    }

    val EXTERNAL_DECLARATION by object : DiagnosticGroup("External Declaration") {
        val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT by error<KtDeclaration>(PositioningStrategy.ABSTRACT_MODIFIER)
        val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY by error<KtDeclaration>(PositioningStrategy.EXTERNAL_MODIFIER)
        val EXTERNAL_DECLARATION_IN_INTERFACE by error<KtDeclaration>(PositioningStrategy.EXTERNAL_MODIFIER)
        val EXTERNAL_DECLARATION_CANNOT_BE_INLINED by error<KtDeclaration>(PositioningStrategy.EXTERNAL_MODIFIER)
    }

    val REPEATABLE by object : DiagnosticGroup("Repeatable Annotations") {
        val NON_SOURCE_REPEATED_ANNOTATION by error<KtAnnotationEntry>()
        val REPEATED_ANNOTATION_WITH_CONTAINER by error<KtAnnotationEntry> {
            parameter<ClassId>("name")
            parameter<ClassId>("explicitContainerName")
        }

        val REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR by error<KtAnnotationEntry> {
            parameter<ClassId>("container")
            parameter<ClassId>("annotation")
        }
        val REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR by error<KtAnnotationEntry> {
            parameter<ClassId>("container")
            parameter<Name>("nonDefault")
        }
        val REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR by error<KtAnnotationEntry> {
            parameter<ClassId>("container")
            parameter<String>("retention")
            parameter<ClassId>("annotation")
            parameter<String>("annotationRetention")
        }
        val REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR by error<KtAnnotationEntry> {
            parameter<ClassId>("container")
            parameter<ClassId>("annotation")
        }
        val REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR by error<KtAnnotationEntry>()
    }

    val SUSPENSION_POINT by object : DiagnosticGroup("Suspension Point") {
        val SUSPENSION_POINT_INSIDE_CRITICAL_SECTION by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirCallableSymbol<*>>("function")
        }
    }

    val INLINE by object : DiagnosticGroup("Inline") {
        val INLINE_FROM_HIGHER_PLATFORM by error<PsiElement> {
            parameter<String>("inlinedBytecodeVersion")
            parameter<String>("currentModuleBytecodeVersion")
        }
    }

    val MISC by object : DiagnosticGroup("Misc") {
        val INAPPLICABLE_JVM_FIELD by error<KtAnnotationEntry> {
            parameter<String>("message")
        }
        val INAPPLICABLE_JVM_FIELD_WARNING by warning<KtAnnotationEntry> {
            parameter<String>("message")
        }
        val IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE by warning<KtElement> {
            parameter<ConeKotlinType>("type")
        }
        val SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS by warning<KtElement> {
            parameter<ConeKotlinType>("type")
        }
        val SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE by deprecationError<PsiElement>(ProhibitSynchronizationByValueClassesAndPrimitives) {
            parameter<ConeKotlinType>("valueClassOrPrimitive")
        }
        val JVM_SYNTHETIC_ON_DELEGATE by error<KtAnnotationEntry>()
        val SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR by error<PsiElement>()
        val SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR by error<PsiElement>(PositioningStrategy.SPREAD_OPERATOR)
        val JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE by error<PsiElement>()
        val NO_REFLECTION_IN_CLASS_PATH by warning<PsiElement>()
        val SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<FirNamedFunctionSymbol>("originalSymbol")
            parameter<Name>("functionName")
        }
        val JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<FirPropertySymbol>("kotlinProperty")
        }
        val MISSING_BUILT_IN_DECLARATION by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<FirBasedSymbol<*>>("symbol")
        }
        val DANGEROUS_CHARACTERS by warning<KtNamedDeclaration>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<String>("characters")
        }
    }
}
