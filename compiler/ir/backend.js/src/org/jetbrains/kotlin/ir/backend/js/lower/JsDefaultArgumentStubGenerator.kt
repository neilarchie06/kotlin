/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.defaultArgumentsDispatchFunction
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

class JsDefaultArgumentStubGenerator(context: JsIrBackendContext) :
    DefaultArgumentStubGenerator<JsIrBackendContext>(
        context = context,
        factory = JsDefaultArgumentFunctionFactory(context),
        skipExternalMethods = true,
        forceSetOverrideSymbols = false
    ) {

    private fun IrBuilderWithScope.createDefaultResolutionExpression(
        defaultExpression: IrExpression?,
        toParameter: IrValueParameter,
    ): IrExpression? {
        return defaultExpression?.let {
            irIfThenElse(
                toParameter.type,
                irEqeqeqWithoutBox(
                    irGet(toParameter, toParameter.type),
                    this@JsDefaultArgumentStubGenerator.context.getVoid()
                ),
                it,
                irGet(toParameter)
            )
        }
    }

    private fun IrBuilderWithScope.createResolutionStatement(
        parameter: IrValueParameter,
        defaultExpression: IrExpression?,
    ): IrSetValue? {
        return createDefaultResolutionExpression(defaultExpression, parameter)?.let {
            JsIrBuilder.buildSetValue(parameter.symbol, it)
        }
    }

    private fun IrFunction.introduceDefaultResolution(): IrFunction {
        this.defaultArgumentsDispatchFunction?.let { return it }
        val irBuilder = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        val variables = hashMapOf<IrValueParameter, IrValueParameter>()

        parameters = parameters.memoryOptimizedMap { param ->
            param.takeIf { it.defaultValue != null }
                ?.copyTo(this, isAssignable = true, origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER)
                ?.also { new -> variables[param] = new } ?: param
        }

        val valueRemapper = VariableRemapper(variables)
        for (parameter in parameters) {
            parameter.defaultValue?.transformChildrenVoid(valueRemapper)
        }

        val blockBody = body as? IrBlockBody

        if (blockBody != null && variables.isNotEmpty()) {
            blockBody.transformChildrenVoid(valueRemapper)
            blockBody.statements.addAll(0, parameters.mapNotNull {
                irBuilder.createResolutionStatement(it, it.defaultValue?.expression)
            })
        }

        return also {
            it.defaultArgumentsDispatchFunction = it
        }
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrFunction || declaration.isExternalOrInheritedFromExternal()) {
            return null
        }

        if (declaration.hasDefaultArgs() && (declaration is IrConstructor || declaration.isTopLevel)) {
            return listOf(declaration.introduceDefaultResolution())
        }

        val (originalFun, defaultFunStub) = super.transformFlat(declaration) ?: return null

        if (originalFun !is IrFunction || defaultFunStub !is IrFunction) {
            return listOf(originalFun, defaultFunStub)
        }

        if (!defaultFunStub.isFakeOverride) {
            with(defaultFunStub) {
                parameters.forEach {
                    if (it.defaultValue != null) {
                        it.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
                    }
                    it.defaultValue = null
                }

                if (originalFun.isExported(context)) {
                    context.additionalExportedDeclarations.add(defaultFunStub)

                    if (!originalFun.hasAnnotation(JsAnnotations.jsNameFqn)) {
                        originalFun.annotations = originalFun.annotations memoryOptimizedPlus originalFun.generateJsNameAnnotationCall()
                    }
                }
            }
        }

        val (exportAnnotations, irrelevantAnnotations) = originalFun.annotations
            .map { it.deepCopyWithSymbols(originalFun as? IrDeclarationParent) }
            .partition {
                it.isAnnotation(JsAnnotations.jsExportFqn) || (it.isAnnotation(JsAnnotations.jsNameFqn))
            }

        originalFun.annotations = irrelevantAnnotations
        defaultFunStub.annotations = exportAnnotations
        originalFun.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT

        return listOf(originalFun, defaultFunStub)
    }

    override fun IrFunction.generateDefaultStubBody(originalDeclaration: IrFunction): IrBody {
        val ctx = context
        val irBuilder = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        val variables = originalDeclaration.parameters.associateByTo(hashMapOf(), { it }, { parameters[it.indexInParameters] })

        return irBuilder.irBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            +parameters.zip(originalDeclaration.parameters)
                .mapNotNull { (new, original) ->
                    createResolutionStatement(
                        new,
                        original.defaultValue?.expression?.transform(VariableRemapper(variables), null),
                    )
                }

            val wrappedFunctionCall = irCall(originalDeclaration, JsStatementOrigins.IMPLEMENTATION_DELEGATION_CALL).apply {
                passTypeArgumentsFrom(originalDeclaration)
                arguments.assignFrom(originalDeclaration.parameters) {
                    irGet(variables[it] ?: parameters[it.indexInParameters])
                }
            }

            var superContextValueParam: IrValueParameter? = null

            val superFunCall = runIf(originalDeclaration.dispatchReceiverParameter != null) {
                val superContext = parameters.last().also {
                    superContextValueParam = it
                }
                val realOverrideTarget = originalDeclaration.realOverrideTarget.takeIf {
                    it !is IrOverridableMember || it.modality !== Modality.ABSTRACT
                }
                if (realOverrideTarget?.parentClassOrNull?.isInterface == true) {
                    irCall(realOverrideTarget).apply {
                        for (i in 1..<wrappedFunctionCall.arguments.size) {
                            arguments[i] = wrappedFunctionCall.arguments[i]?.deepCopyWithSymbols()
                        }
                    }
                } else {
                    irCall(ctx.intrinsics.jsCall).apply {
                        arguments[0] = wrappedFunctionCall.arguments[0]?.deepCopyWithSymbols()
                        arguments[1] = irCall(ctx.intrinsics.jsContexfulRef).apply {
                            arguments[0] = irGet(superContext)
                            arguments[1] = irRawFunctionReference(ctx.dynamicType, originalDeclaration.symbol)
                        }
                        arguments[2] = irVararg(ctx.dynamicType, buildList {
                            wrappedFunctionCall.arguments.drop(1).mapNotNullTo(this) { it?.deepCopyWithSymbols() }
                        })
                    }
                }
            }

            +irReturn(
                if (superFunCall == null) {
                    wrappedFunctionCall
                } else {
                    irIfThenElse(
                        originalDeclaration.returnType,
                        irEqeqeqWithoutBox(irGet(superContextValueParam!!), ctx.getVoid()),
                        wrappedFunctionCall,
                        superFunCall
                    )
                }
            )
        }
    }

    private fun IrFunction.generateJsNameAnnotationCall(): IrConstructorCall {
        val builder = context.createIrBuilder(symbol, startOffset, endOffset)

        return with(context) {
            builder.irCall(intrinsics.jsNameAnnotationSymbol.constructors.single())
                .apply {
                    arguments[0] = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.stringType, name.identifier)
                }
        }
    }

    private fun IrConstructorCall.isAnnotation(name: FqName): Boolean {
        return symbol.owner.parentAsClass.fqNameWhenAvailable == name
    }

    private fun IrFunction.hasDefaultArgs(): Boolean =
        parameters.any { it.defaultValue != null }
}
