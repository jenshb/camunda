/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.test.performance;

import org.camunda.optimize.test.it.extension.ElasticSearchIntegrationTestExtensionRule;
import org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtensionRule;
import org.camunda.optimize.test.util.PropertyUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractCleanupTest {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractCleanupTest.class);

  private static final Properties properties = PropertyUtil.loadProperties("static-cleanup-test.properties");

  @RegisterExtension
  @Order(1)
  protected static ElasticSearchIntegrationTestExtensionRule elasticSearchIntegrationTestExtensionRule = new ElasticSearchIntegrationTestExtensionRule();
  @RegisterExtension
  @Order(2)
  protected static EmbeddedOptimizeExtensionRule embeddedOptimizeExtensionRule = new EmbeddedOptimizeExtensionRule();

  protected static long maxCleanupDurationInMin = Long.parseLong(properties.getProperty(
    "cleanup.test.max.duration.in.min",
    "240"
  ));

  static {
    elasticSearchIntegrationTestExtensionRule.disableCleanup();
    embeddedOptimizeExtensionRule.setResetImportOnStart(false);
    embeddedOptimizeExtensionRule.setupOptimize();
  }

  protected static void importData() {
    final OffsetDateTime importStart = OffsetDateTime.now();
    logger.info("Starting import of engine data to Optimize...");
    embeddedOptimizeExtensionRule.importAllEngineData();
    elasticSearchIntegrationTestExtensionRule.refreshAllOptimizeIndices();
    OffsetDateTime afterImport = OffsetDateTime.now();
    long importDurationInMinutes = ChronoUnit.MINUTES.between(importStart, afterImport);
    logger.info("Import took [ " + importDurationInMinutes + " ] min");
  }

  protected static void runCleanupAndAssertFinishedWithinTimeout() throws InterruptedException, TimeoutException {
    logger.info("Starting History Cleanup...");
    final ExecutorService cleanupExecutorService = Executors.newSingleThreadExecutor();
    cleanupExecutorService.execute(
      () -> embeddedOptimizeExtensionRule.getCleanupScheduler().runCleanup()
    );
    cleanupExecutorService.shutdown();
    boolean wasAbleToFinishImportInTime = cleanupExecutorService.awaitTermination(
      maxCleanupDurationInMin, TimeUnit.MINUTES
    );
    logger.info(".. History cleanup finished, timed out {} ", !wasAbleToFinishImportInTime);
    if (!wasAbleToFinishImportInTime) {
      throw new TimeoutException("Import was not able to finish import in " + maxCleanupDurationInMin + " minutes!");
    }
  }


}
