/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.immutable;

public interface ScheduledTaskState {

  DistributionState getDistributionState();

  MessageState getMessageState();

  TimerInstanceState getTimerState();

  JobState getJobState();

  DeploymentState getDeploymentState();

  PendingMessageSubscriptionState getPendingMessageSubscriptionState();

  PendingProcessMessageSubscriptionState getPendingProcessMessageSubscriptionState();

  UserTaskState getUserTaskState();
}