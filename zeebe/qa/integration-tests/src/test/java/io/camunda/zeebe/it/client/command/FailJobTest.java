/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.client.command;

import static io.camunda.zeebe.test.util.record.RecordingExporter.jobRecords;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.client.api.command.ClientStatusException;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.zeebe.broker.test.EmbeddedBrokerRule;
import io.camunda.zeebe.it.util.GrpcClientRule;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.test.util.BrokerClassRuleHelper;
import java.time.Duration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public final class FailJobTest {

  private static final EmbeddedBrokerRule BROKER_RULE = new EmbeddedBrokerRule();
  private static final GrpcClientRule CLIENT_RULE = new GrpcClientRule(BROKER_RULE);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(BROKER_RULE).around(CLIENT_RULE);

  @Rule public final BrokerClassRuleHelper helper = new BrokerClassRuleHelper();

  private String jobType;
  private long jobKey;

  @Before
  public void init() {
    jobType = helper.getJobType();
    CLIENT_RULE.createSingleJob(jobType);

    jobKey = activateJob().getKey();
  }

  @Test
  public void shouldFailJobWithRemainingRetries() {
    // when
    CLIENT_RULE.getClient().newFailCommand(jobKey).retries(2).send().join();

    // then
    final Record<JobRecordValue> record =
        jobRecords(JobIntent.FAILED).withRecordKey(jobKey).getFirst();
    Assertions.assertThat(record.getValue()).hasRetries(2).hasErrorMessage("");

    final var activatedJob = activateJob();
    assertThat(activatedJob.getKey()).isEqualTo(jobKey);
    assertThat(activatedJob.getRetries()).isEqualTo(2);
  }

  @Test
  public void shouldFailJobWithErrorMessage() {
    // when
    CLIENT_RULE.getClient().newFailCommand(jobKey).retries(0).errorMessage("test").send().join();

    // then
    final Record<JobRecordValue> record =
        jobRecords(JobIntent.FAILED).withRecordKey(jobKey).getFirst();
    Assertions.assertThat(record.getValue()).hasRetries(0).hasErrorMessage("test");
  }

  @Test
  public void shouldFailJobWithRetryBackOff() {
    // when
    final Duration backoffTimeout = Duration.ofSeconds(30);
    CLIENT_RULE
        .getClient()
        .newFailCommand(jobKey)
        .retries(1)
        .retryBackoff(backoffTimeout)
        .send()
        .join();

    // then
    final Record<JobRecordValue> beforeRecurRecord =
        jobRecords(JobIntent.FAILED).withRecordKey(jobKey).getFirst();
    Assertions.assertThat(beforeRecurRecord.getValue())
        .hasRetries(1)
        .hasRetryBackoff(backoffTimeout.toMillis());
  }

  @Test
  public void shouldRejectIfJobIsAlreadyCompleted() {
    // given
    CLIENT_RULE.getClient().newCompleteCommand(jobKey).send().join();

    // when
    final var expectedMessage =
        String.format("Expected to fail job with key '%d', but no such job was found", jobKey);

    assertThatThrownBy(
            () -> CLIENT_RULE.getClient().newFailCommand(jobKey).retries(1).send().join())
        .isInstanceOf(ClientStatusException.class)
        .hasMessageContaining(expectedMessage);
  }

  private ActivatedJob activateJob() {
    final var activateResponse =
        CLIENT_RULE
            .getClient()
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(1)
            .send()
            .join();

    assertThat(activateResponse.getJobs())
        .describedAs("Expected one job to be activated")
        .hasSize(1);

    return activateResponse.getJobs().get(0);
  }
}
