/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.operate.exporter.handlers;

import static io.camunda.zeebe.operate.exporter.util.OperateExportUtil.tenantOrDefault;

import io.camunda.operate.entities.SequenceFlowEntity;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.schema.templates.SequenceFlowTemplate;
import io.camunda.operate.store.elasticsearch.NewElasticsearchBatchRequest;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.List;

public class SequenceFlowHandler
    implements ExportHandler<SequenceFlowEntity, ProcessInstanceRecordValue> {

  private static final String ID_PATTERN = "%s_%s";

  private final SequenceFlowTemplate sequenceFlowTemplate;

  public SequenceFlowHandler(final SequenceFlowTemplate sequenceFlowTemplate) {
    this.sequenceFlowTemplate = sequenceFlowTemplate;
  }

  @Override
  public ValueType getHandledValueType() {
    return ValueType.PROCESS_INSTANCE;
  }

  @Override
  public Class<SequenceFlowEntity> getEntityType() {
    return SequenceFlowEntity.class;
  }

  @Override
  public boolean handlesRecord(final Record<ProcessInstanceRecordValue> record) {
    return record.getIntent().name().equals(ProcessInstanceIntent.SEQUENCE_FLOW_TAKEN.name());
  }

  @Override
  public List<String> generateIds(final Record<ProcessInstanceRecordValue> record) {
    final var recordValue = record.getValue();
    return List.of(
        String.format(ID_PATTERN, recordValue.getProcessInstanceKey(), recordValue.getElementId()));
  }

  @Override
  public SequenceFlowEntity createNewEntity(final String id) {
    return new SequenceFlowEntity().setId(id);
  }

  @Override
  public void updateEntity(
      final Record<ProcessInstanceRecordValue> record, final SequenceFlowEntity entity) {
    final var recordValue = record.getValue();
    entity
        .setId(
            String.format(
                ID_PATTERN, recordValue.getProcessInstanceKey(), recordValue.getElementId()))
        .setProcessInstanceKey(recordValue.getProcessInstanceKey())
        .setProcessDefinitionKey(recordValue.getProcessDefinitionKey())
        .setBpmnProcessId(recordValue.getBpmnProcessId())
        .setActivityId(recordValue.getElementId())
        .setTenantId(tenantOrDefault(recordValue.getTenantId()));
  }

  @Override
  public void flush(
      final SequenceFlowEntity entity, final NewElasticsearchBatchRequest batchRequest)
      throws PersistenceException {
    batchRequest.add(getIndexName(), entity);
  }

  @Override
  public String getIndexName() {
    return sequenceFlowTemplate.getFullQualifiedName();
  }
}