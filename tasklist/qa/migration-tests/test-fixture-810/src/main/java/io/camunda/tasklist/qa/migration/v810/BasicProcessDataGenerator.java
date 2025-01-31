/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.qa.migration.v810;

import io.camunda.client.CamundaClient;
import io.camunda.tasklist.qa.util.TestContext;
import io.camunda.tasklist.qa.util.ZeebeTestUtil;
import io.camunda.tasklist.schema.templates.TaskTemplate;
import io.camunda.tasklist.util.ThreadUtil;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** It is considered that Zeebe and Elasticsearch are running. */
@Component
public class BasicProcessDataGenerator {

  public static final String PROCESS_BPMN_PROCESS_ID = "basicProcess";
  public static final int PROCESS_INSTANCE_COUNT = 51;
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicProcessDataGenerator.class);

  //  private static final DateTimeFormatter ARCHIVER_DATE_TIME_FORMATTER =
  // DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
  /**
   * CamundaClient must not be reused between different test fixtures, as this may be different
   * versions of client in the future.
   */
  private CamundaClient camundaClient;

  @Autowired
  @Qualifier("tasklistEsClient")
  private RestHighLevelClient esClient;

  private final Random random = new Random();

  private List<Long> processInstanceKeys = new ArrayList<>();

  private void init(final TestContext testContext) {
    camundaClient =
        CamundaClient.newClientBuilder()
            .gatewayAddress(testContext.getExternalZeebeContactPoint())
            .usePlaintext()
            .build();
  }

  public void createData(final TestContext testContext) throws Exception {
    init(testContext);
    try {
      final OffsetDateTime dataGenerationStart = OffsetDateTime.now();
      LOGGER.info("Starting generating data for process {}", PROCESS_BPMN_PROCESS_ID);

      deployProcess();
      processInstanceKeys = startProcessInstances(PROCESS_INSTANCE_COUNT);

      waitUntilAllDataAreImported();

      claimAllTasks();

      try {
        esClient.indices().refresh(new RefreshRequest("tasklist-*"), RequestOptions.DEFAULT);
      } catch (final IOException e) {
        LOGGER.error("Error in refreshing indices", e);
      }
      LOGGER.info(
          "Data generation completed in: {} s",
          ChronoUnit.SECONDS.between(dataGenerationStart, OffsetDateTime.now()));
      testContext.addProcess(PROCESS_BPMN_PROCESS_ID);
    } finally {
      closeClients();
    }
  }

  private void claimAllTasks() {
    final UpdateByQueryRequest updateRequest =
        new UpdateByQueryRequest(getMainIndexNameFor(TaskTemplate.INDEX_NAME))
            .setQuery(QueryBuilders.matchAllQuery())
            .setScript(
                new Script(
                    ScriptType.INLINE,
                    "painless",
                    "ctx._source.assignee = 'demo'",
                    Collections.emptyMap()))
            .setRefresh(true);
    try {
      esClient.updateByQuery(updateRequest, RequestOptions.DEFAULT);
    } catch (final ElasticsearchException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void closeClients() {
    if (camundaClient != null) {
      camundaClient.close();
      camundaClient = null;
    }
  }

  private void waitUntilAllDataAreImported() throws IOException {
    LOGGER.info("Wait till data is imported.");
    final SearchRequest searchRequest = new SearchRequest(getAliasFor(TaskTemplate.INDEX_NAME));
    long loadedProcessInstances = 0;
    int count = 0;
    final int maxWait = 101;
    while (PROCESS_INSTANCE_COUNT > loadedProcessInstances && count < maxWait) {
      count++;
      loadedProcessInstances = countEntitiesFor(searchRequest);
      ThreadUtil.sleepFor(1000L);
    }
    if (count == maxWait) {
      throw new RuntimeException("Waiting for loading process instances failed: Timeout");
    }
  }

  private List<Long> startProcessInstances(final int numberOfProcessInstances) {
    for (int i = 0; i < numberOfProcessInstances; i++) {
      final String bpmnProcessId = PROCESS_BPMN_PROCESS_ID;
      final long processInstanceKey =
          ZeebeTestUtil.startProcessInstance(
              camundaClient, bpmnProcessId, "{\"var1\": \"value1\"}");
      LOGGER.debug("Started processInstance {} for process {}", processInstanceKey, bpmnProcessId);
      processInstanceKeys.add(processInstanceKey);
    }
    LOGGER.info("{} processInstances started", processInstanceKeys.size());
    return processInstanceKeys;
  }

  private void deployProcess() {
    final String bpmnProcessId = PROCESS_BPMN_PROCESS_ID;
    final String processDefinitionKey =
        ZeebeTestUtil.deployProcess(
            camundaClient, createModel(bpmnProcessId), bpmnProcessId + ".bpmn");
    LOGGER.info("Deployed process {} with key {}", bpmnProcessId, processDefinitionKey);
  }

  private BpmnModelInstance createModel(final String bpmnProcessId) {
    return Bpmn.createExecutableProcess(bpmnProcessId)
        .startEvent("start")
        .serviceTask("task1")
        .zeebeJobType("io.camunda.zeebe:userTask")
        .zeebeInput("=var1", "varIn")
        .zeebeOutput("=varOut", "var2")
        .serviceTask("task2")
        .zeebeJobType("task2")
        .serviceTask("task3")
        .zeebeJobType("task3")
        .serviceTask("task4")
        .zeebeJobType("task4")
        .serviceTask("task5")
        .zeebeJobType("task5")
        .endEvent()
        .done();
  }

  private long countEntitiesFor(final SearchRequest searchRequest) throws IOException {
    searchRequest.source().size(1000);
    final SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
    return searchResponse.getHits().getTotalHits().value;
  }

  private String getAliasFor(final String index) {
    return String.format("tasklist-%s-*_alias", index);
  }

  private String getMainIndexNameFor(final String index) {
    return String.format("tasklist-%s-*_", index);
  }
}
