/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.TestScopeRenderer.renderForTests
import org.jetbrains.kotlin.analysis.api.useSiteSession
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractScopeContextForPositionTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getTopmostSelectedElementOfType<KtElement>(mainFile)

        copyAwareAnalyzeForTest(element) { contextElement ->
            val scopeContext = mainFile.scopeContext(contextElement)

            val scopeContextStringRepresentation = renderForTests(contextElement, scopeContext)
            val scopeContextStringRepresentationPretty = renderForTests(contextElement, scopeContext, printPretty = true)

            testServices.assertions.assertEqualsToTestOutputFile(scopeContextStringRepresentation)
            testServices.assertions.assertEqualsToTestOutputFile(scopeContextStringRepresentationPretty, extension = ".pretty.txt")
        }
    }

    context(_: KaSession)
    private fun renderForTests(
        element: KtElement,
        scopeContext: KaScopeContext,
        printPretty: Boolean = false,
    ): String = prettyPrint {
        appendLine("element: ${element.text}")
        renderForTests(useSiteSession, scopeContext, this@prettyPrint, printPretty) { scopeKind ->
            scopeKind !is KaScopeKind.DefaultSimpleImportingScope && scopeKind !is KaScopeKind.DefaultStarImportingScope
        }
    }
}
