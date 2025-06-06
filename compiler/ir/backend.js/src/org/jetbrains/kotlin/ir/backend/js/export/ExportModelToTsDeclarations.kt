/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf


private const val NonNullable = "NonNullable"
private const val declare = "declare "
private const val declareExported = "export $declare"

private const val getInstance = "getInstance"

private const val Metadata = $$"$metadata$"
private const val MetadataType = "type"
private const val MetadataConstructor = "constructor"
private const val Nullable = "Nullable"
private const val ObjectInheritanceIntrinsic = "KtSingleton"

@JvmInline
value class TypeScriptFragment(val raw: String)

fun List<ExportedDeclaration>.toTypeScriptFragment(moduleKind: ModuleKind): TypeScriptFragment {
    return ExportModelToTsDeclarations(moduleKind).generateTypeScriptFragment(this)
}

fun List<TypeScriptFragment>.joinTypeScriptFragments(): TypeScriptFragment {
    return TypeScriptFragment(joinToString("\n") { it.raw })
}

fun List<TypeScriptFragment>.toTypeScript(name: String, moduleKind: ModuleKind): String {
    return ExportModelToTsDeclarations(moduleKind).generateTypeScript(name, this)
}

// TODO: Support module kinds other than plain
class ExportModelToTsDeclarations(private val moduleKind: ModuleKind) {
    private val isEsModules = moduleKind == ModuleKind.ES
    private val intrinsicsPrefix = if (moduleKind == ModuleKind.PLAIN) "" else "declare "
    private val topLevelPrefix = if (moduleKind == ModuleKind.PLAIN) "" else declareExported
    private val indent: String = if (moduleKind == ModuleKind.PLAIN) "    " else ""

    fun generateTypeScript(name: String, declarations: List<TypeScriptFragment>): String {
        val internalNamespace = """
            type $Nullable<T> = T | null | undefined
            ${intrinsicsPrefix}function $ObjectInheritanceIntrinsic<T>(): T & (abstract new() => any);
        """.trimIndent().prependIndent(indent) + "\n"

        val declarationsDts = internalNamespace + declarations.joinTypeScriptFragments().raw

        val namespaceName = sanitizeName(name, withHash = false)

        return when (moduleKind) {
            ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
            ModuleKind.AMD, ModuleKind.COMMON_JS, ModuleKind.ES -> declarationsDts
            ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
        }
    }

    fun generateTypeScriptFragment(declarations: List<ExportedDeclaration>): TypeScriptFragment {
        return TypeScriptFragment(declarations.toTypeScript())
    }

    private fun List<ExportedDeclaration>.toTypeScript(): String {
        return joinToString("\n") {
            it.toTypeScript(indent = indent, prefix = topLevelPrefix)
        }
    }

    private fun List<ExportedDeclaration>.toTypeScript(indent: String): String =
        joinToString("") { it.toTypeScript(indent) + "\n" }

    private fun ExportedDeclaration.toTypeScript(indent: String, prefix: String = ""): String =
        attributes.toTypeScript(indent) + indent + when (this) {
            is ErrorDeclaration -> generateTypeScriptString()
            is ExportedConstructor -> generateTypeScriptString(indent)
            is ExportedConstructSignature -> generateTypeScriptString(indent)
            is ExportedNamespace -> generateTypeScriptString(indent, prefix)
            is ExportedFunction -> generateTypeScriptString(indent, prefix)
            is ExportedRegularClass -> generateTypeScriptString(indent, prefix)
            is ExportedProperty -> generateTypeScriptString(indent, prefix)
            is ExportedObject -> generateTypeScriptString(indent, prefix)
        }

    private fun Iterable<ExportedAttribute>.toTypeScript(indent: String): String {
        return joinToString("\n") { it.toTypeScript(indent) }
            .run { if (isNotEmpty()) plus("\n") else this }
    }

    private fun ExportedAttribute.toTypeScript(indent: String): String {
        return when (this) {
            is ExportedAttribute.DeprecatedAttribute -> indent + tsDeprecated(message)
        }
    }

    private fun ErrorDeclaration.generateTypeScriptString(): String {
        return "/* ErrorDeclaration: $message */"
    }

    private fun ExportedNamespace.generateTypeScriptString(indent: String, prefix: String): String {
        return "${prefix.takeIf { !isPrivate } ?: "declare "}namespace $name {\n" + declarations.toTypeScript("$indent    ") + "$indent}"
    }

    private fun ExportedConstructor.generateTypeScriptString(indent: String): String {
        return "${visibility.keyword}constructor(${parameters.generateTypeScriptString(indent)});"
    }

    private fun ExportedConstructSignature.generateTypeScriptString(indent: String): String {
        return "new(${parameters.generateTypeScriptString(indent)}): ${returnType.toTypeScript(indent)};"
    }

    private fun ExportedProperty.generateTypeScriptString(indent: String, prefix: String): String {
        val extraIndent = "$indent    "
        val optional = if (isOptional) "?" else ""
        val containsUnresolvedChar = !name.isValidES5Identifier()
        val memberName = if (containsUnresolvedChar) "\"$name\"" else name
        val isObjectGetter = irGetter?.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION

        val typeToTypeScript = type.toTypeScript(if (!isMember && isEsModules && isObjectGetter) extraIndent else indent)

        return if (isMember) {
            val static = if (isStatic) "static " else ""
            val abstract = if (isAbstract) "abstract " else ""
            val visibility = if (isProtected) "protected " else ""

            if (isField) {
                val readonly = if (!mutable) "readonly " else ""
                "$prefix$visibility$static$abstract$readonly$memberName$optional: $typeToTypeScript;"
            } else {
                val getter = "$prefix$visibility$static${abstract}get $memberName(): $typeToTypeScript;"
                val setter = runIf(mutable) { "\n$indent$prefix$visibility$static${abstract}set $memberName(value: $typeToTypeScript);" }
                getter + setter.orEmpty()
            }
        } else {
            when {
                containsUnresolvedChar -> ""
                isEsModules && !isQualified -> {
                    if (isObjectGetter) {
                        "${prefix}const $name: {\n${extraIndent}getInstance(): $typeToTypeScript;\n};"
                    } else {
                        val getter = "get(): $typeToTypeScript;"
                        val setter = runIf(mutable) { " set(value: $typeToTypeScript): void;" }
                        "${prefix}const $name: { $getter${setter.orEmpty()} };"
                    }
                }

                else -> {
                    val keyword = if (mutable) "let " else "const "
                    "$prefix$keyword$memberName$optional: $typeToTypeScript;"
                }
            }
        }
    }

    private fun ExportedFunction.generateTypeScriptString(indent: String, prefix: String): String {
        val visibility = if (isProtected) "protected " else ""

        val keyword: String = when {
            isMember -> when {
                isStatic -> "static "
                isAbstract -> "abstract "
                else -> ""
            }

            else -> "function "
        }

        val renderedParameters = parameters.generateTypeScriptString(indent)
        val renderedTypeParameters = if (typeParameters.isNotEmpty()) {
            "<" + typeParameters.joinToString(", ") { it.toTypeScript(indent) } + ">"
        } else {
            ""
        }

        val renderedReturnType = returnType.toTypeScript(indent)
        val containsUnresolvedChar = !name.isValidES5Identifier()

        val escapedName = when {
            isMember && containsUnresolvedChar -> "\"$name\""
            else -> name
        }

        return if (!isMember && containsUnresolvedChar) {
            ""
        } else {
            "$prefix$visibility$keyword$escapedName$renderedTypeParameters($renderedParameters): $renderedReturnType;"
        }
    }

    private fun ExportedObject.generateTypeScriptString(indent: String, prefix: String): String {
        val shouldGenerateObjectWithGetInstance =
            isEsModules && !ir.isEffectivelyExternal() && (ir.parent as? IrClass).let { it == null || (it.isInterface && !ir.isCompanion) }

        val constructorTypeReference =
            if (shouldGenerateObjectWithGetInstance) MetadataConstructor else "$name.$Metadata.$MetadataConstructor"

        val substitutionOfObjectTypeToItsShapeClass = mapOf<ExportedType, ExportedType>(
            ExportedType.TypeOf(ExportedType.ClassType(name, emptyList(), ir))
                    to ExportedType.ClassType(MetadataConstructor, emptyList())
        )

        val classContainingShape = ExportedRegularClass(
            name = MetadataConstructor,
            isInterface = false,
            isAbstract = true,
            requireMetadata = false,
            ir = ir,
            typeParameters = emptyList(),
            nestedClasses = emptyList(),
            superClasses = superClasses.map { it.replaceTypes(substitutionOfObjectTypeToItsShapeClass) },
            superInterfaces = superInterfaces,
            members = members + ExportedConstructor(emptyList(), ExportedVisibility.PRIVATE),
        )

        val classContainingType = ExportedRegularClass(
            name = if (shouldGenerateObjectWithGetInstance) MetadataType else name,
            isInterface = false,
            isAbstract = true,
            ir = ir,
            requireMetadata = false,
            typeParameters = typeParameters,
            nestedClasses = nestedClasses,
            superClasses = listOfNotNull(
                ExportedType.ObjectsParentType(ExportedType.ClassType(constructorTypeReference, emptyList()))
            ),
            members = listOf(ExportedConstructor(emptyList(), ExportedVisibility.PRIVATE))
        )

        val extraClassWithGetInstanceMethod = runIf(shouldGenerateObjectWithGetInstance) {
            ExportedRegularClass(
                name = name,
                isInterface = false,
                isAbstract = true,
                ir = ir,
                requireMetadata = false,
                typeParameters = typeParameters,
                nestedClasses = emptyList(),
                superClasses = emptyList(),
                members = listOf(
                    ExportedProperty(
                        name = getInstance,
                        type = ExportedType.Function(
                            emptyList(),
                            ExportedType.TypeOf(ExportedType.ClassType("$name.$Metadata.$MetadataType", emptyList()))
                        ),
                        isMember = true,
                        mutable = false,
                        isStatic = true,
                        isField = true
                    ),
                    ExportedConstructor(emptyList(), ExportedVisibility.PRIVATE),
                )
            )
        }

        val metadataMembers =
            if (shouldGenerateObjectWithGetInstance) listOf(classContainingType, classContainingShape) else listOf(classContainingShape)

        val objectClass = (extraClassWithGetInstanceMethod ?: classContainingType).generateTypeScriptString(indent, prefix)
        val objectMetadata = generateMetadataNamespace(name, metadataMembers).toTypeScript(indent, prefix)

        return "$objectClass\n$objectMetadata"
    }

    private fun ExportedRegularClass.generateTypeScriptString(indent: String, prefix: String): String {
        val keyword = if (isInterface) "interface" else "class"
        val (interfaceCompanions, allNestedClasses) = nestedClasses.partition { isInterface && it.ir.isCompanion }
        val superInterfacesKeyword = if (isInterface) "extends" else "implements"

        val superClassClause = superClasses.toExtendsClause(indent)
        val superInterfacesClause = superInterfaces.toImplementsClause(superInterfacesKeyword, indent)

        val members = members.map {
            if (!ir.isInner || it !is ExportedFunction || !it.isStatic) {
                it
            } else {
                // Remove $outer argument from secondary constructors of inner classes
                it.copy(parameters = it.parameters.drop(1))
            }
        }

        val (innerClasses, nonInnerClasses) = allNestedClasses.partition { it.ir.isInner }
        val innerClassesProperties = innerClasses.map { it.toReadonlyProperty() }
        val membersString = (members + innerClassesProperties)
            .joinToString("") { it.toTypeScript("$indent    ") + "\n" }

        // If there are no exported constructors, add a private constructor to disable default one
        val privateCtorString = if (!isInterface && !isAbstract && members.none { it is ExportedConstructor }) {
            "$indent    private constructor();\n"
        } else {
            ""
        }

        val renderedTypeParameters = if (typeParameters.isNotEmpty()) {
            "<" + typeParameters.joinToString(", ") { it.toTypeScript(indent) } + ">"
        } else {
            ""
        }

        val modifiers = if (isAbstract && !isInterface) "abstract " else ""

        val bodyString = privateCtorString + membersString + indent

        val realNestedClasses = nonInnerClasses + innerClasses.map { it.withProtectedConstructors() }

        val klassExport =
            "$prefix$modifiers$keyword $name$renderedTypeParameters$superClassClause$superInterfacesClause {\n$bodyString}"
        val staticsExport =
            if (realNestedClasses.isNotEmpty()) "\n" + ExportedNamespace(name, realNestedClasses).toTypeScript(indent, prefix) else ""

        val metadataString = runIf(requireMetadata) {
            val constructorProperty = ExportedProperty(
                name = MetadataConstructor,
                type = ExportedType.ConstructorType(
                    typeParameters,
                    ExportedType.ClassType(name, typeParameters.map { it.copy(constraint = null) }, ir)
                ),
                mutable = false,
                isQualified = true
            )
            generateMetadataNamespace(name, listOf(constructorProperty))
                .toTypeScript(indent, prefix)
                .let { "\n" + it }
        }.orEmpty()

        val interfaceCompanionsString = if (interfaceCompanions.isNotEmpty()) "\n" + interfaceCompanions.joinToString("\n") {
            (it as ExportedObject).copy(typeParameters = typeParameters).toTypeScript(indent, prefix)
        } else ""

        return if (name.isValidES5Identifier()) klassExport + metadataString + staticsExport + interfaceCompanionsString else ""
    }

    private fun List<ExportedType>.toExtendsClause(indent: String): String {
        if (isEmpty()) return ""

        val implicitlyExportedClasses = filterIsInstance<ExportedType.ImplicitlyExportedType>()
        val implicitlyExportedClassesString = implicitlyExportedClasses.joinToString(", ") { it.toTypeScript(indent, true) }

        return if (implicitlyExportedClasses.count() == count()) {
            " /* extends $implicitlyExportedClassesString */"
        } else {
            val originallyDefinedSuperClass = implicitlyExportedClassesString.takeIf { it.isNotEmpty() }?.let { "/* $it */ " }.orEmpty()
            val transitivelyDefinedSuperClass = when (val parentType = single { it !is ExportedType.ImplicitlyExportedType }) {
                is ExportedType.ClassType -> ExportedType.ClassType(
                    "${parentType.name}.$Metadata.$MetadataConstructor",
                    parentType.arguments,
                    parentType.ir
                )
                else -> parentType
            }.toTypeScript(indent, false)

            " extends $originallyDefinedSuperClass$transitivelyDefinedSuperClass"
        }
    }

    private fun List<ExportedType>.toImplementsClause(superInterfacesKeyword: String, indent: String): String {
        val (exportedInterfaces, nonExportedInterfaces) = partition { it !is ExportedType.ImplicitlyExportedType }
        val listOfNonExportedInterfaces = nonExportedInterfaces.joinToString(", ") {
            (it as ExportedType.ImplicitlyExportedType).type.toTypeScript(indent, true)
        }
        return when {
            exportedInterfaces.isEmpty() && nonExportedInterfaces.isNotEmpty() ->
                " /* $superInterfacesKeyword $listOfNonExportedInterfaces */"

            exportedInterfaces.isNotEmpty() -> {
                val nonExportedInterfacesTsString = if (nonExportedInterfaces.isNotEmpty()) "/*, $listOfNonExportedInterfaces */" else ""
                " $superInterfacesKeyword " + exportedInterfaces.joinToString(", ") { it.toTypeScript(indent) } + nonExportedInterfacesTsString
            }

            else -> ""
        }
    }

    private fun ExportedClass.withProtectedConstructors(): ExportedRegularClass {
        return (this as ExportedRegularClass).copy(members = members.map {
            if (it !is ExportedConstructor || it.isProtected) {
                it
            } else {
                it.copy(visibility = ExportedVisibility.PROTECTED)
            }
        })
    }

    private fun ExportedClass.toReadonlyProperty(): ExportedProperty {
        val innerClassReference = ir.asNestedClassAccess()
        val allPublicConstructors = members.asSequence()
            .filterIsInstance<ExportedConstructor>()
            .filterNot { it.isProtected }
            .map {
                ExportedConstructSignature(
                    parameters = it.parameters.drop(1),
                    returnType = ExportedType.TypeParameter(innerClassReference),
                )
            }
            .toList()

        val type = ExportedType.IntersectionType(
            ExportedType.InlineInterfaceType(allPublicConstructors),
            ExportedType.TypeOf(ExportedType.ClassType(innerClassReference, emptyList(), ir))
        )

        return ExportedProperty(name = name, type = type, mutable = false, isMember = true)
    }

    private fun List<ExportedParameter>.generateTypeScriptString(indent: String): String {
        var couldBeOptional = true
        val parameters = foldRight(mutableListOf<String>()) { it, acc ->
            if (!it.hasDefaultValue) couldBeOptional = false
            acc.apply { add(0, it.toTypeScript(indent, couldBeOptional)) }
        }
        return parameters.joinToString(", ")
    }

    private fun ExportedParameter.toTypeScript(indent: String, couldBeOptional: Boolean): String {
        val name = sanitizeName(name, withHash = false)
        val type = if (hasDefaultValue && !couldBeOptional) {
            ExportedType.UnionType(type, ExportedType.Primitive.Undefined)
        } else type
        val questionMark = if (hasDefaultValue && couldBeOptional) "?" else ""
        return "$name$questionMark: ${type.toTypeScript(indent)}"
    }

    private fun IrClass.asNestedClassAccess(): String {
        val name = getJsNameOrKotlinName().identifier
        if (parent !is IrClass) return name
        return "${parentAsClass.asNestedClassAccess()}.$name"
    }

    private fun ExportedType.toTypeScript(indent: String, isInCommentContext: Boolean = false): String = when (this) {
        is ExportedType.Primitive -> typescript
        is ExportedType.Array -> "Array<${elementType.toTypeScript(indent, isInCommentContext)}>"
        is ExportedType.ObjectsParentType -> "$ObjectInheritanceIntrinsic<${constructor.toTypeScript(indent, isInCommentContext)}>()"

        is ExportedType.Function -> "(" + parameterTypes
            .withIndex()
            .joinToString(", ") { (index, type) ->
                "p$index: ${type.toTypeScript(indent, isInCommentContext)}"
            } + ") => " + returnType.toTypeScript(indent, isInCommentContext)

        is ExportedType.ConstructorType ->
            "abstract new " + (if (typeParameters.isNotEmpty()) "<${
                typeParameters.joinToString(", ") {
                    it.toTypeScript(
                        indent,
                        isInCommentContext
                    )
                }
            }>" else "") + "() => ${returnType.toTypeScript(indent, isInCommentContext)}"

        is ExportedType.ClassType -> {
            val classTypeReference = if (ir?.isObject == true && !ir.isEffectivelyExternal() && isEsModules) "$name.$Metadata.$MetadataType" else name
            classTypeReference + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toTypeScript(indent, isInCommentContext) }}>" else ""
        }


        is ExportedType.TypeOf ->
            "typeof ${classType.toTypeScript(indent, isInCommentContext)}"

        is ExportedType.ErrorType -> if (isInCommentContext) comment else "any /*$comment*/"
        is ExportedType.Nullable -> "$Nullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.NonNullable -> "$NonNullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.InlineInterfaceType -> {
            members.joinToString(prefix = "{\n", postfix = "$indent}", separator = "") { it.toTypeScript("$indent    ") + "\n" }
        }

        is ExportedType.IntersectionType -> {
            lhs.toTypeScript(indent) + " & " + rhs.toTypeScript(indent, isInCommentContext)
        }

        is ExportedType.UnionType -> {
            lhs.toTypeScript(indent) + " | " + rhs.toTypeScript(indent, isInCommentContext)
        }

        is ExportedType.LiteralType.StringLiteralType -> "\"$value\""
        is ExportedType.LiteralType.NumberLiteralType -> value.toString()
        is ExportedType.ImplicitlyExportedType -> {
            val typeString = type.toTypeScript("", true)
            if (isInCommentContext) {
                typeString
            } else {
                val superTypeString = exportedSupertype.toTypeScript(indent)
                superTypeString.let { if (exportedSupertype is ExportedType.IntersectionType) "($it)" else it } + "/* $typeString */"
            }
        }

        is ExportedType.PropertyType -> "${container.toTypeScript(indent, isInCommentContext)}[${
            propertyName.toTypeScript(
                indent,
                isInCommentContext
            )
        }]"

        is ExportedType.TypeParameter -> if (constraint == null) {
            name
        } else {
            "$name extends ${constraint.toTypeScript(indent, isInCommentContext)}"
        }
    }

    private fun generateMetadataNamespace(name: String, members: List<ExportedDeclaration>): ExportedNamespace =
        ExportedNamespace("$name.$Metadata", members)
            .apply { attributes += ExportedAttribute.DeprecatedAttribute("$Metadata is used for internal purposes, please don't use it in your code, because it can be removed at any moment") }

    private fun tsDeprecated(message: String): String {
        return "/** @deprecated $message */"
    }
}
