/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.appliers;

import static io.camunda.zeebe.util.buffer.BufferUtil.bufferAsArray;
import static io.camunda.zeebe.util.buffer.BufferUtil.bufferAsString;
import static io.camunda.zeebe.util.buffer.BufferUtil.wrapString;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.engine.state.deployment.PersistedForm;
import io.camunda.zeebe.engine.state.mutable.MutableFormState;
import io.camunda.zeebe.engine.state.mutable.MutableProcessingState;
import io.camunda.zeebe.engine.util.ProcessingStateExtension;
import io.camunda.zeebe.protocol.impl.record.value.deployment.FormRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;
import io.camunda.zeebe.test.util.Strings;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ProcessingStateExtension.class)
public class FormCreatedApplierTest {

  private static final String TENANT_1 = "tenant1";
  private static final String TENANT_2 = "tenant2";
  private KeyGenerator keyGenerator;
  private MutableProcessingState processingState;
  private MutableFormState formState;
  private FormCreatedApplier formCreatedApplier;

  @BeforeEach
  public void setup() {
    formState = processingState.getFormState();
    formCreatedApplier = new FormCreatedApplier(formState);
    keyGenerator = processingState.getKeyGenerator();
  }

  @Test
  void shouldStoreForm() {
    // given
    final var formRecord = sampleFormRecord();
    formCreatedApplier.applyState(formRecord.getFormKey(), formRecord);

    // when
    final var maybePersistedForm = formState.findFormByKey(formRecord.getFormKey(), TENANT_1);

    // then
    assertThat(maybePersistedForm).hasValueSatisfying(isEqualToFormRecord(formRecord));
  }

  @Test
  void shouldFindLatestByFormId() {
    // given
    final var formRecord1 = sampleFormRecord();
    formCreatedApplier.applyState(formRecord1.getFormKey(), formRecord1);

    final var formRecord2 = sampleFormRecord(2L, "form-id", 2, 2L, TENANT_1);
    formCreatedApplier.applyState(formRecord2.getFormKey(), formRecord2);

    // when
    final var maybePersistedForm =
        formState.findLatestFormById(formRecord1.getFormIdBuffer(), TENANT_1);

    // then
    assertThat(maybePersistedForm).hasValueSatisfying(isEqualToFormRecord(formRecord2));
  }

  @Test
  void shouldFindFormByFormIdAndDeploymentKey() {
    // given
    final var form1Version1 = sampleFormRecord(1L, "form-1", 1, 1L, TENANT_1);
    formCreatedApplier.applyState(form1Version1.getFormKey(), form1Version1);

    final var form2Version1 = sampleFormRecord(2L, "form-2", 1, 1L, TENANT_1);
    formCreatedApplier.applyState(form2Version1.getFormKey(), form2Version1);

    final var form1Version2 = sampleFormRecord(3L, "form-1", 2, 2L, TENANT_1);
    formCreatedApplier.applyState(form1Version2.getFormKey(), form1Version2);

    // when
    final var maybePersistedForm1 =
        formState.findFormByIdAndDeploymentKey(wrapString("form-1"), 1L, TENANT_1);
    final var maybePersistedForm2 =
        formState.findFormByIdAndDeploymentKey(wrapString("form-2"), 1L, TENANT_1);
    final var maybePersistedForm3 =
        formState.findFormByIdAndDeploymentKey(wrapString("form-1"), 2L, TENANT_1);
    final var maybePersistedForm4 =
        formState.findFormByIdAndDeploymentKey(wrapString("form-2"), 2L, TENANT_1);

    // then
    assertThat(maybePersistedForm1).hasValueSatisfying(isEqualToFormRecord(form1Version1));
    assertThat(maybePersistedForm2).hasValueSatisfying(isEqualToFormRecord(form2Version1));
    assertThat(maybePersistedForm3).hasValueSatisfying(isEqualToFormRecord(form1Version2));
    assertThat(maybePersistedForm4).isEmpty(); // form-2 not deployed again
  }

  @Test
  public void shouldPutFormForDifferentTenants() {
    // given
    final var formKey = keyGenerator.nextKey();
    final var deploymentKey = keyGenerator.nextKey();
    final var formId = Strings.newRandomValidBpmnId();
    final var version = 1;
    final var tenant1Form = sampleFormRecord(formKey, formId, version, deploymentKey, TENANT_1);
    final var tenant2Form = sampleFormRecord(formKey, formId, version, deploymentKey, TENANT_2);

    // when
    formCreatedApplier.applyState(tenant1Form.getFormKey(), tenant1Form);
    formCreatedApplier.applyState(tenant2Form.getFormKey(), tenant2Form);

    // then
    var form1 = formState.findFormByKey(formKey, TENANT_1).orElseThrow();
    var form2 = formState.findFormByKey(formKey, TENANT_2).orElseThrow();
    assertPersistedForm(form1, formKey, formId, version, deploymentKey, TENANT_1);
    assertPersistedForm(form2, formKey, formId, version, deploymentKey, TENANT_2);

    form1 = formState.findLatestFormById(wrapString(formId), TENANT_1).orElseThrow();
    form2 = formState.findLatestFormById(wrapString(formId), TENANT_2).orElseThrow();
    assertPersistedForm(form1, formKey, formId, version, deploymentKey, TENANT_1);
    assertPersistedForm(form2, formKey, formId, version, deploymentKey, TENANT_2);

    form1 =
        formState
            .findFormByIdAndDeploymentKey(wrapString(formId), deploymentKey, TENANT_1)
            .orElseThrow();
    form2 =
        formState
            .findFormByIdAndDeploymentKey(wrapString(formId), deploymentKey, TENANT_2)
            .orElseThrow();
    assertPersistedForm(form1, formKey, formId, version, deploymentKey, TENANT_1);
    assertPersistedForm(form2, formKey, formId, version, deploymentKey, TENANT_2);
  }

  private FormRecord sampleFormRecord() {
    return sampleFormRecord(1L, "form-id", 1, 1L, TENANT_1);
  }

  private FormRecord sampleFormRecord(
      final long key,
      final String id,
      final int version,
      final long deploymentKey,
      final String tenant) {
    return new FormRecord()
        .setFormKey(key)
        .setFormId(id)
        .setVersion(version)
        .setResourceName("resourceName")
        .setResource(wrapString("resource"))
        .setChecksum(wrapString("checksum"))
        .setTenantId(tenant)
        .setDeploymentKey(deploymentKey);
  }

  private void assertPersistedForm(
      final PersistedForm persistedForm,
      final long expectedKey,
      final String expectedId,
      final int expectedVersion,
      final long expectedDeploymentKey,
      final String expectedTenant) {
    assertThat(persistedForm)
        .extracting(
            PersistedForm::getFormKey,
            form -> bufferAsString(form.getFormId()),
            PersistedForm::getVersion,
            PersistedForm::getDeploymentKey,
            PersistedForm::getTenantId)
        .describedAs("Gets correct form for tenant")
        .containsExactly(
            expectedKey, expectedId, expectedVersion, expectedDeploymentKey, expectedTenant);
  }

  private Consumer<PersistedForm> isEqualToFormRecord(final FormRecord record) {
    return persistedForm -> {
      assertThat(bufferAsString(persistedForm.getFormId())).isEqualTo(record.getFormId());
      assertThat(persistedForm.getVersion()).isEqualTo(record.getVersion());
      assertThat(persistedForm.getFormKey()).isEqualTo(record.getFormKey());
      assertThat(bufferAsString(persistedForm.getResourceName()))
          .isEqualTo(record.getResourceName());
      assertThat(bufferAsArray(persistedForm.getChecksum())).isEqualTo(record.getChecksum());
      assertThat(bufferAsArray(persistedForm.getResource())).isEqualTo(record.getResource());
      assertThat(persistedForm.getDeploymentKey()).isEqualTo(record.getDeploymentKey());
    };
  }
}
