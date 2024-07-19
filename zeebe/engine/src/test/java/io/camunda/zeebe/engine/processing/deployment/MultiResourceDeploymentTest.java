/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.intent.DecisionIntent;
import io.camunda.zeebe.protocol.record.intent.DecisionRequirementsIntent;
import io.camunda.zeebe.protocol.record.intent.FormIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessIntent;
import io.camunda.zeebe.protocol.record.value.deployment.DecisionRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.DecisionRequirementsMetadataValue;
import io.camunda.zeebe.protocol.record.value.deployment.FormMetadataValue;
import io.camunda.zeebe.protocol.record.value.deployment.ProcessMetadataValue;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.util.function.Consumer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MultiResourceDeploymentTest {

  private static final BpmnModelInstance PROCESS_V1 =
      Bpmn.createExecutableProcess("process1").startEvent("v1").endEvent().done();
  private static final String FORM_V1 = "/form/test-form-1.form";
  private static final String FORM_V2 = "/form/test-form-1_v2.form";
  private static final String DMN_V1 = "/dmn/decision-table.dmn";
  private static final String DMN_V2 = "/dmn/decision-table_v2.dmn";

  @RunWith(Parameterized.class)
  public static class DeploymentWithChangesTest {

    @Rule public final EngineRule engine = EngineRule.singlePartition();

    @Parameter(0)
    public String testName;

    @Parameter(1)
    public BpmnModelInstance bpmn1;

    @Parameter(2)
    public BpmnModelInstance bpmn2;

    @Parameter(3)
    public String dmn1;

    @Parameter(4)
    public String dmn2;

    @Parameter(5)
    public String form1;

    @Parameter(6)
    public String form2;

    @Parameters(name = "{0}")
    public static Object[][] parameters() {
      return new Object[][] {
        new Object[] {
          "BPMN has changed",
          PROCESS_V1,
          Bpmn.createExecutableProcess("process1").startEvent("v2").endEvent().done(),
          DMN_V1,
          DMN_V1,
          FORM_V1,
          FORM_V1
        },
        new Object[] {"DMN has changed", PROCESS_V1, PROCESS_V1, DMN_V1, DMN_V2, FORM_V1, FORM_V1},
        new Object[] {"Form has changed", PROCESS_V1, PROCESS_V1, DMN_V1, DMN_V1, FORM_V1, FORM_V2}
      };
    }

    @Test
    public void shouldCreateNewVersionsOfAllResourcesIfAtLeastOneResourceHasChanged() {
      // given
      final var firstDeployment =
          engine
              .deployment()
              .withXmlResource(bpmn1)
              .withXmlClasspathResource(dmn1)
              .withJsonClasspathResource(form1)
              .deploy()
              .getValue();
      final var processV1 = firstDeployment.getProcessesMetadata().getFirst();
      final var drgV1 = firstDeployment.getDecisionRequirementsMetadata().getFirst();
      final var decisionV1 = firstDeployment.getDecisionsMetadata().getFirst();
      final var formV1 = firstDeployment.getFormMetadata().getFirst();

      // when
      final var secondDeployment =
          engine
              .deployment()
              .withXmlResource(bpmn2)
              .withXmlClasspathResource(dmn2)
              .withJsonClasspathResource(form2)
              .deploy()
              .getValue();

      // then
      final var processesMetadata = secondDeployment.getProcessesMetadata();
      assertThat(processesMetadata).hasSize(1);
      final var processV2 = processesMetadata.getFirst();
      assertThat(processV2).satisfies(expectedProcessMetadata(processV1.getProcessDefinitionKey()));

      final var decisionRequirementsMetadata = secondDeployment.getDecisionRequirementsMetadata();
      assertThat(decisionRequirementsMetadata).hasSize(1);
      final var drgV2 = decisionRequirementsMetadata.getFirst();
      assertThat(drgV2)
          .satisfies(expectedDecisionRequirementsMetadata(drgV1.getDecisionRequirementsKey()));

      final var decisionsMetadata = secondDeployment.getDecisionsMetadata();
      assertThat(decisionsMetadata).hasSize(1);
      final var decisionV2 = decisionsMetadata.getFirst();
      assertThat(decisionV2)
          .satisfies(
              expectedDecisionMetadata(
                  drgV2.getDecisionRequirementsKey(), decisionV1.getDecisionKey()));

      final var formMetadata = secondDeployment.getFormMetadata();
      assertThat(formMetadata).hasSize(1);
      final var formV2 = formMetadata.getFirst();
      assertThat(formV2).satisfies(expectedFormMetadata(formV1.getFormKey()));

      assertNewProcessCreatedRecord(processV2.getProcessDefinitionKey());
      assertNewDecisionRequirementsCreatedRecord(drgV2.getDecisionRequirementsKey());
      assertNewDecisionCreatedRecord(
          decisionV2.getDecisionKey(), drgV2.getDecisionRequirementsKey());
      assertNewFormCreatedRecord(formV2.getFormKey());
    }

    private static Consumer<ProcessMetadataValue> expectedProcessMetadata(
        final long previousProcessDefinitionKey) {
      return process ->
          Assertions.assertThat(process)
              .hasVersion(2)
              .isNotDuplicate()
              .extracting(
                  ProcessMetadataValue::getProcessDefinitionKey, InstanceOfAssertFactories.LONG)
              .isGreaterThan(previousProcessDefinitionKey);
    }

    private static Consumer<DecisionRequirementsMetadataValue> expectedDecisionRequirementsMetadata(
        final long previousDecisionRequirementsKey) {
      return drg ->
          Assertions.assertThat(drg)
              .hasDecisionRequirementsVersion(2)
              .isNotDuplicate()
              .extracting(
                  DecisionRequirementsMetadataValue::getDecisionRequirementsKey,
                  InstanceOfAssertFactories.LONG)
              .isGreaterThan(previousDecisionRequirementsKey);
    }

    private static Consumer<DecisionRecordValue> expectedDecisionMetadata(
        final long expectedDecisionRequirementsKey, final long previousDecisionKey) {
      return decision ->
          Assertions.assertThat(decision)
              .hasVersion(2)
              .hasDecisionRequirementsKey(expectedDecisionRequirementsKey)
              .isNotDuplicate()
              .extracting(DecisionRecordValue::getDecisionKey, InstanceOfAssertFactories.LONG)
              .isGreaterThan(previousDecisionKey);
    }

    private static Consumer<FormMetadataValue> expectedFormMetadata(final long previousFormKey) {
      return form ->
          Assertions.assertThat(form)
              .hasVersion(2)
              .isNotDuplicate()
              .extracting(FormMetadataValue::getFormKey, InstanceOfAssertFactories.LONG)
              .isGreaterThan(previousFormKey);
    }

    private static void assertNewProcessCreatedRecord(final long expectedProcessDefinitionKey) {
      assertThat(
              RecordingExporter.processRecords()
                  .withIntent(ProcessIntent.CREATED)
                  .limit(2)
                  .getLast())
          .satisfies(
              record -> {
                assertThat(record.getKey()).isEqualTo(expectedProcessDefinitionKey);
                assertThat(record.getValue().getProcessDefinitionKey())
                    .isEqualTo(expectedProcessDefinitionKey);
                assertThat(record.getValue().getVersion()).isEqualTo(2);
              });
    }

    private static void assertNewDecisionRequirementsCreatedRecord(
        final long expectedDecisionRequirementsKey) {
      assertThat(
              RecordingExporter.decisionRequirementsRecords()
                  .withIntent(DecisionRequirementsIntent.CREATED)
                  .limit(2)
                  .getLast())
          .satisfies(
              record -> {
                assertThat(record.getKey()).isEqualTo(expectedDecisionRequirementsKey);
                assertThat(record.getValue().getDecisionRequirementsKey())
                    .isEqualTo(expectedDecisionRequirementsKey);
                assertThat(record.getValue().getDecisionRequirementsVersion()).isEqualTo(2);
              });
    }

    private static void assertNewDecisionCreatedRecord(
        final long expectedDecisionKey, final long expectedDecisionRequirementsKey) {
      assertThat(
              RecordingExporter.decisionRecords()
                  .withIntent(DecisionIntent.CREATED)
                  .limit(2)
                  .getLast())
          .satisfies(
              record -> {
                assertThat(record.getKey()).isEqualTo(expectedDecisionKey);
                assertThat(record.getValue().getDecisionKey()).isEqualTo(expectedDecisionKey);
                assertThat(record.getValue().getDecisionRequirementsKey())
                    .isEqualTo(expectedDecisionRequirementsKey);
                assertThat(record.getValue().getVersion()).isEqualTo(2);
              });
    }

    private static void assertNewFormCreatedRecord(final long expectedFormKey) {
      assertThat(RecordingExporter.formRecords().withIntent(FormIntent.CREATED).limit(2).getLast())
          .satisfies(
              record -> {
                assertThat(record.getKey()).isEqualTo(expectedFormKey);
                assertThat(record.getValue().getFormKey()).isEqualTo(expectedFormKey);
                assertThat(record.getValue().getVersion()).isEqualTo(2);
              });
    }
  }

  public static class DeploymentWithNoChangesTest {

    @Rule public final EngineRule engine = EngineRule.singlePartition();

    @Test
    public void shouldNotCreateNewVersionsIfNoResourceHasChanged() {
      // given
      final var firstDeployment =
          engine
              .deployment()
              .withXmlResource(PROCESS_V1)
              .withXmlClasspathResource(DMN_V1)
              .withJsonClasspathResource(FORM_V1)
              .deploy()
              .getValue();
      final var processV1 = firstDeployment.getProcessesMetadata().getFirst();
      final var drgV1 = firstDeployment.getDecisionRequirementsMetadata().getFirst();
      final var decisionV1 = firstDeployment.getDecisionsMetadata().getFirst();
      final var formV1 = firstDeployment.getFormMetadata().getFirst();

      // when
      final var secondDeployment =
          engine
              .deployment()
              // deploy the exact same resources again
              .withXmlResource(PROCESS_V1)
              .withXmlClasspathResource(DMN_V1)
              .withJsonClasspathResource(FORM_V1)
              .deploy()
              .getValue();

      // then
      assertThat(secondDeployment.getProcessesMetadata())
          .singleElement()
          .satisfies(
              metadata ->
                  Assertions.assertThat(metadata)
                      .hasVersion(1)
                      .isDuplicate()
                      .extracting(
                          ProcessMetadataValue::getProcessDefinitionKey,
                          InstanceOfAssertFactories.LONG)
                      .isEqualTo(processV1.getProcessDefinitionKey()));
      assertThat(secondDeployment.getDecisionRequirementsMetadata())
          .singleElement()
          .satisfies(
              metadata ->
                  Assertions.assertThat(metadata)
                      .hasDecisionRequirementsVersion(1)
                      .isDuplicate()
                      .extracting(
                          DecisionRequirementsMetadataValue::getDecisionRequirementsKey,
                          InstanceOfAssertFactories.LONG)
                      .isEqualTo(drgV1.getDecisionRequirementsKey()));
      assertThat(secondDeployment.getDecisionsMetadata())
          .singleElement()
          .satisfies(
              metadata ->
                  Assertions.assertThat(metadata)
                      .hasVersion(1)
                      .isDuplicate()
                      .extracting(
                          DecisionRecordValue::getDecisionKey,
                          DecisionRecordValue::getDecisionRequirementsKey)
                      .containsExactly(
                          decisionV1.getDecisionKey(), drgV1.getDecisionRequirementsKey()));
      assertThat(secondDeployment.getFormMetadata())
          .singleElement()
          .satisfies(
              metadata ->
                  Assertions.assertThat(metadata)
                      .hasVersion(1)
                      .isDuplicate()
                      .extracting(FormMetadataValue::getFormKey)
                      .isEqualTo(formV1.getFormKey()));

      assertThat(RecordingExporter.processRecords().withIntent(ProcessIntent.CREATED).limit(2))
          .map(record -> record.getValue().getProcessDefinitionKey())
          .containsExactly(processV1.getProcessDefinitionKey());
      assertThat(
              RecordingExporter.decisionRequirementsRecords()
                  .withIntent(DecisionRequirementsIntent.CREATED)
                  .limit(2))
          .map(record -> record.getValue().getDecisionRequirementsKey())
          .containsExactly(drgV1.getDecisionRequirementsKey());
      assertThat(RecordingExporter.decisionRecords().withIntent(DecisionIntent.CREATED).limit(2))
          .map(record -> record.getValue().getDecisionKey())
          .containsExactly(decisionV1.getDecisionKey());
      assertThat(RecordingExporter.formRecords().withIntent(FormIntent.CREATED).limit(2))
          .map(record -> record.getValue().getFormKey())
          .containsExactly(formV1.getFormKey());
    }
  }
}
