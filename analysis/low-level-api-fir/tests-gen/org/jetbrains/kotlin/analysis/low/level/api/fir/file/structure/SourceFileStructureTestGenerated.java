/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/low-level-api-fir/testData/fileStructure")
@TestDataPath("$PROJECT_ROOT")
public class SourceFileStructureTestGenerated extends AbstractSourceFileStructureTest {
  @Test
  public void testAllFilesPresentInFileStructure() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/low-level-api-fir/testData/fileStructure"), Pattern.compile("^(.+)\\.(kt)$"), null, true);
  }

  @Test
  @TestMetadata("annonymousClass.kt")
  public void testAnnonymousClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/annonymousClass.kt");
  }

  @Test
  @TestMetadata("callInsideLambdaInsideSuperCallAndExplicitConstructor.kt")
  public void testCallInsideLambdaInsideSuperCallAndExplicitConstructor() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/callInsideLambdaInsideSuperCallAndExplicitConstructor.kt");
  }

  @Test
  @TestMetadata("callInsideLambdaInsideSuperCallAndImplicitConstructor.kt")
  public void testCallInsideLambdaInsideSuperCallAndImplicitConstructor() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/callInsideLambdaInsideSuperCallAndImplicitConstructor.kt");
  }

  @Test
  @TestMetadata("callInsideLambdaInsideSuperCallFromSecondaryConstructor.kt")
  public void testCallInsideLambdaInsideSuperCallFromSecondaryConstructor() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/callInsideLambdaInsideSuperCallFromSecondaryConstructor.kt");
  }

  @Test
  @TestMetadata("callInsideLambdaInsideSuperCallFromSingleSecondaryConstructor.kt")
  public void testCallInsideLambdaInsideSuperCallFromSingleSecondaryConstructor() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/callInsideLambdaInsideSuperCallFromSingleSecondaryConstructor.kt");
  }

  @Test
  @TestMetadata("class.kt")
  public void testClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/class.kt");
  }

  @Test
  @TestMetadata("class2.kt")
  public void testClass2() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/class2.kt");
  }

  @Test
  @TestMetadata("classMemberProperty.kt")
  public void testClassMemberProperty() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/classMemberProperty.kt");
  }

  @Test
  @TestMetadata("constructorParameter.kt")
  public void testConstructorParameter() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/constructorParameter.kt");
  }

  @Test
  @TestMetadata("constructorParameter2.kt")
  public void testConstructorParameter2() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/constructorParameter2.kt");
  }

  @Test
  @TestMetadata("constructorParameterWithAnnotations.kt")
  public void testConstructorParameterWithAnnotations() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/constructorParameterWithAnnotations.kt");
  }

  @Test
  @TestMetadata("constructors.kt")
  public void testConstructors() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/constructors.kt");
  }

  @Test
  @TestMetadata("contextParameters.kt")
  public void testContextParameters() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/contextParameters.kt");
  }

  @Test
  @TestMetadata("danglingAnnotationClassLevel.kt")
  public void testDanglingAnnotationClassLevel() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingAnnotationClassLevel.kt");
  }

  @Test
  @TestMetadata("danglingAnnotationClassLevelFunction.kt")
  public void testDanglingAnnotationClassLevelFunction() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingAnnotationClassLevelFunction.kt");
  }

  @Test
  @TestMetadata("danglingAnnotationEnumEntry.kt")
  public void testDanglingAnnotationEnumEntry() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingAnnotationEnumEntry.kt");
  }

  @Test
  @TestMetadata("danglingAnnotationInMiddle.kt")
  public void testDanglingAnnotationInMiddle() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingAnnotationInMiddle.kt");
  }

  @Test
  @TestMetadata("danglingAnnotationTopLevel.kt")
  public void testDanglingAnnotationTopLevel() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingAnnotationTopLevel.kt");
  }

  @Test
  @TestMetadata("danglingAnnotationTopLevelFunction.kt")
  public void testDanglingAnnotationTopLevelFunction() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingAnnotationTopLevelFunction.kt");
  }

  @Test
  @TestMetadata("danglingContextParameter.kt")
  public void testDanglingContextParameter() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/danglingContextParameter.kt");
  }

  @Test
  @TestMetadata("declarationsInPropertyInit.kt")
  public void testDeclarationsInPropertyInit() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/declarationsInPropertyInit.kt");
  }

  @Test
  @TestMetadata("enum.kt")
  public void testEnum() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/enum.kt");
  }

  @Test
  @TestMetadata("enumClass.kt")
  public void testEnumClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/enumClass.kt");
  }

  @Test
  @TestMetadata("enumClassWithBody.kt")
  public void testEnumClassWithBody() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/enumClassWithBody.kt");
  }

  @Test
  @TestMetadata("funWithoutTypes.kt")
  public void testFunWithoutTypes() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/funWithoutTypes.kt");
  }

  @Test
  @TestMetadata("functionValueParameter.kt")
  public void testFunctionValueParameter() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/functionValueParameter.kt");
  }

  @Test
  @TestMetadata("functionWithImplicitType.kt")
  public void testFunctionWithImplicitType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/functionWithImplicitType.kt");
  }

  @Test
  @TestMetadata("functionWithImplicitTypeAndFunctionInside.kt")
  public void testFunctionWithImplicitTypeAndFunctionInside() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/functionWithImplicitTypeAndFunctionInside.kt");
  }

  @Test
  @TestMetadata("functionWithImplicitTypeAndFunctionInsideLocalClass.kt")
  public void testFunctionWithImplicitTypeAndFunctionInsideLocalClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/functionWithImplicitTypeAndFunctionInsideLocalClass.kt");
  }

  @Test
  @TestMetadata("functionWithImplicitTypeAndPropertyInside.kt")
  public void testFunctionWithImplicitTypeAndPropertyInside() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/functionWithImplicitTypeAndPropertyInside.kt");
  }

  @Test
  @TestMetadata("functionalType.kt")
  public void testFunctionalType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/functionalType.kt");
  }

  @Test
  @TestMetadata("initBlock.kt")
  public void testInitBlock() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/initBlock.kt");
  }

  @Test
  @TestMetadata("lambda.kt")
  public void testLambda() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/lambda.kt");
  }

  @Test
  @TestMetadata("lambdaInImplicitFunBody.kt")
  public void testLambdaInImplicitFunBody() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/lambdaInImplicitFunBody.kt");
  }

  @Test
  @TestMetadata("lambdaInImplicitPropertyBody.kt")
  public void testLambdaInImplicitPropertyBody() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/lambdaInImplicitPropertyBody.kt");
  }

  @Test
  @TestMetadata("lambdasInWithBodyFunction.kt")
  public void testLambdasInWithBodyFunction() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/lambdasInWithBodyFunction.kt");
  }

  @Test
  @TestMetadata("localClass.kt")
  public void testLocalClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localClass.kt");
  }

  @Test
  @TestMetadata("localClass2.kt")
  public void testLocalClass2() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localClass2.kt");
  }

  @Test
  @TestMetadata("localDeclarationsInAccessor.kt")
  public void testLocalDeclarationsInAccessor() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localDeclarationsInAccessor.kt");
  }

  @Test
  @TestMetadata("localFun.kt")
  public void testLocalFun() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localFun.kt");
  }

  @Test
  @TestMetadata("localFunctionWithImplicitType.kt")
  public void testLocalFunctionWithImplicitType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localFunctionWithImplicitType.kt");
  }

  @Test
  @TestMetadata("localProperty.kt")
  public void testLocalProperty() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localProperty.kt");
  }

  @Test
  @TestMetadata("localUnitFunction.kt")
  public void testLocalUnitFunction() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/localUnitFunction.kt");
  }

  @Test
  @TestMetadata("memberFunctions.kt")
  public void testMemberFunctions() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/memberFunctions.kt");
  }

  @Test
  @TestMetadata("memberProperties.kt")
  public void testMemberProperties() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/memberProperties.kt");
  }

  @Test
  @TestMetadata("memberTypeAlias.kt")
  public void testMemberTypeAlias() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/memberTypeAlias.kt");
  }

  @Test
  @TestMetadata("multipleTopLevelClasses.kt")
  public void testMultipleTopLevelClasses() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/multipleTopLevelClasses.kt");
  }

  @Test
  @TestMetadata("multipleTopLevelFunctionsWithImplicitTypes.kt")
  public void testMultipleTopLevelFunctionsWithImplicitTypes() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/multipleTopLevelFunctionsWithImplicitTypes.kt");
  }

  @Test
  @TestMetadata("multipleTopLevelUnitFunctions.kt")
  public void testMultipleTopLevelUnitFunctions() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/multipleTopLevelUnitFunctions.kt");
  }

  @Test
  @TestMetadata("nestedClases.kt")
  public void testNestedClases() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/nestedClases.kt");
  }

  @Test
  @TestMetadata("nestedClasesWithFun.kt")
  public void testNestedClasesWithFun() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/nestedClasesWithFun.kt");
  }

  @Test
  @TestMetadata("nestedClasses.kt")
  public void testNestedClasses() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/nestedClasses.kt");
  }

  @Test
  @TestMetadata("propertyAccessors.kt")
  public void testPropertyAccessors() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/propertyAccessors.kt");
  }

  @Test
  @TestMetadata("propertyWithGetterAndSetter.kt")
  public void testPropertyWithGetterAndSetter() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/propertyWithGetterAndSetter.kt");
  }

  @Test
  @TestMetadata("propertyWithImplicitTypeAndAnnotationsInsideLocalClass.kt")
  public void testPropertyWithImplicitTypeAndAnnotationsInsideLocalClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/propertyWithImplicitTypeAndAnnotationsInsideLocalClass.kt");
  }

  @Test
  @TestMetadata("propertyWithImplicitTypeAndFieldAnnotationsInsideLocalClass.kt")
  public void testPropertyWithImplicitTypeAndFieldAnnotationsInsideLocalClass() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/propertyWithImplicitTypeAndFieldAnnotationsInsideLocalClass.kt");
  }

  @Test
  @TestMetadata("propertyWithSetter.kt")
  public void testPropertyWithSetter() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/propertyWithSetter.kt");
  }

  @Test
  @TestMetadata("qualifiedCallInsideSuperCall.kt")
  public void testQualifiedCallInsideSuperCall() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/qualifiedCallInsideSuperCall.kt");
  }

  @Test
  @TestMetadata("secondaryConstructor.kt")
  public void testSecondaryConstructor() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/secondaryConstructor.kt");
  }

  @Test
  @TestMetadata("superCallAnnotation.kt")
  public void testSuperCallAnnotation() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/superCallAnnotation.kt");
  }

  @Test
  @TestMetadata("superCallAnnotation2.kt")
  public void testSuperCallAnnotation2() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/superCallAnnotation2.kt");
  }

  @Test
  @TestMetadata("superClassCall.kt")
  public void testSuperClassCall() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/superClassCall.kt");
  }

  @Test
  @TestMetadata("superType.kt")
  public void testSuperType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/superType.kt");
  }

  @Test
  @TestMetadata("topLevelExpressionBodyFunWithType.kt")
  public void testTopLevelExpressionBodyFunWithType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/topLevelExpressionBodyFunWithType.kt");
  }

  @Test
  @TestMetadata("topLevelExpressionBodyFunWithoutType.kt")
  public void testTopLevelExpressionBodyFunWithoutType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/topLevelExpressionBodyFunWithoutType.kt");
  }

  @Test
  @TestMetadata("topLevelFunWithType.kt")
  public void testTopLevelFunWithType() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/topLevelFunWithType.kt");
  }

  @Test
  @TestMetadata("topLevelProperty.kt")
  public void testTopLevelProperty() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/topLevelProperty.kt");
  }

  @Test
  @TestMetadata("topLevelUnitFun.kt")
  public void testTopLevelUnitFun() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/topLevelUnitFun.kt");
  }

  @Test
  @TestMetadata("typeAlias.kt")
  public void testTypeAlias() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/typeAlias.kt");
  }

  @Test
  @TestMetadata("withoutName.kt")
  public void testWithoutName() {
    runTest("analysis/low-level-api-fir/testData/fileStructure/withoutName.kt");
  }
}
