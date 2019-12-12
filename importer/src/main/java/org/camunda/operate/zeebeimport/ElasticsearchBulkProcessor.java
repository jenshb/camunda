/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.zeebeimport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.camunda.operate.Metrics;
import org.camunda.operate.exceptions.PersistenceException;
import org.camunda.operate.util.ElasticsearchUtil;
import org.camunda.operate.zeebe.ImportValueType;
import org.camunda.operate.zeebeimport.processors.ActivityInstanceZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.EventZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.IncidentZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.ListViewZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.OperationZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.SequenceFlowZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.VariableZeebeRecordProcessor;
import org.camunda.operate.zeebeimport.processors.WorkflowZeebeRecordProcessor;
import org.camunda.operate.zeebe.record.RecordImpl;
import org.camunda.operate.zeebe.record.value.JobRecordValueImpl;
import org.camunda.operate.zeebe.record.value.WorkflowInstanceRecordValueImpl;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.micrometer.core.annotation.Timed;
import io.zeebe.protocol.record.Record;

@Component
public class ElasticsearchBulkProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchBulkProcessor.class);

  @Autowired
  private RestHighLevelClient esClient;

  @Autowired
  private ListViewZeebeRecordProcessor listViewZeebeRecordProcessor;

  @Autowired
  private ActivityInstanceZeebeRecordProcessor activityInstanceZeebeRecordProcessor;

  @Autowired
  private VariableZeebeRecordProcessor variableZeebeRecordProcessor;

  @Autowired
  private OperationZeebeRecordProcessor operationZeebeRecordProcessor;

  @Autowired
  private IncidentZeebeRecordProcessor incidentZeebeRecordProcessor;

  @Autowired
  private WorkflowZeebeRecordProcessor workflowZeebeRecordProcessor;

  @Autowired
  private EventZeebeRecordProcessor eventZeebeRecordProcessor;

  @Autowired
  private SequenceFlowZeebeRecordProcessor sequenceFlowZeebeRecordProcessor;

  @Autowired
  private Metrics metrics;

  public void persistZeebeRecords(ImportBatch importBatch) throws PersistenceException {
    List<Record> zeebeRecords = importBatch.getRecords();
    ImportValueType importValueType = importBatch.getImportValueType();

    logger.debug("Writing [{}] Zeebe records to Elasticsearch", zeebeRecords.size());

    BulkRequest bulkRequest = new BulkRequest();

    switch (importValueType.getValueType()) {
    case WORKFLOW_INSTANCE:
      Map<Long, List<RecordImpl<WorkflowInstanceRecordValueImpl>>> groupedWIRecords = zeebeRecords.stream()
          .map(obj -> (RecordImpl<WorkflowInstanceRecordValueImpl>) obj).collect(Collectors.groupingBy(obj -> obj.getValue().getWorkflowInstanceKey()));
      listViewZeebeRecordProcessor.processWorkflowInstanceRecord(groupedWIRecords, bulkRequest, importBatch);
      Map<Long, List<RecordImpl<WorkflowInstanceRecordValueImpl>>> groupedWIRecordsPerActivity = zeebeRecords.stream()
          .map(obj -> (RecordImpl<WorkflowInstanceRecordValueImpl>) obj).collect(Collectors.groupingBy(obj -> obj.getKey()));
      activityInstanceZeebeRecordProcessor.processWorkflowInstanceRecord(groupedWIRecordsPerActivity, bulkRequest);
      eventZeebeRecordProcessor.processWorkflowInstanceRecord(groupedWIRecordsPerActivity, bulkRequest);
      for (Record record : zeebeRecords) {
        sequenceFlowZeebeRecordProcessor.processSequenceFlowRecord(record, bulkRequest);
      }
      break;
    case INCIDENT:
      // old style
      for (Record record : zeebeRecords) {
        listViewZeebeRecordProcessor.processIncidentRecord(record, bulkRequest);
        activityInstanceZeebeRecordProcessor.processIncidentRecord(record, bulkRequest);
        incidentZeebeRecordProcessor.processIncidentRecord(record, bulkRequest);
        eventZeebeRecordProcessor.processIncidentRecord(record, bulkRequest);
      }
      break;
    case VARIABLE:
      // old style
      for (Record record : zeebeRecords) {
        listViewZeebeRecordProcessor.processVariableRecord(record, bulkRequest);
        variableZeebeRecordProcessor.processVariableRecord(record, bulkRequest);
      }
      break;
    case VARIABLE_DOCUMENT:
      for (Record record : zeebeRecords) {
        operationZeebeRecordProcessor.processVariableDocumentRecords(record, bulkRequest);
      }
      break;
    case DEPLOYMENT:
      for (Record record : zeebeRecords) {
        // deployment records can be processed one by one
        workflowZeebeRecordProcessor.processDeploymentRecord(record, bulkRequest);
      }
      break;
    case JOB:
      // per activity
      Map<Long, List<RecordImpl<JobRecordValueImpl>>> groupedJobRecords = zeebeRecords.stream().map(obj -> (RecordImpl<JobRecordValueImpl>) obj)
          .collect(Collectors.groupingBy(obj -> obj.getValue().getElementInstanceKey()));
      eventZeebeRecordProcessor.processJobRecord(groupedJobRecords, bulkRequest);
      break;
    default:
      logger.debug("Default case triggered for type {}", importValueType.getValueType());
      break;
    }

    try {
      withTimer(() -> {
        ElasticsearchUtil.processBulkRequest(esClient, bulkRequest);
        return null;
      });
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  private void withTimer(Callable<Void> callable) throws Exception {
    metrics.getTimer(Metrics.TIMER_NAME_IMPORT_INDEX_QUERY).recordCallable(callable);
  }

}
