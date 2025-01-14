/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.zeebe.operation;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * This class is intended to provide an abstraction layer for modify process commands that are
 * created and sent to zeebe
 */
@Component
public class ModifyProcessZeebeWrapper {

  private CamundaClient camundaClient;

  public ModifyProcessZeebeWrapper(final CamundaClient camundaClient) {
    this.camundaClient = camundaClient;
  }

  public CamundaClient getCamundaClient() {
    return camundaClient;
  }

  public void setCamundaClient(final CamundaClient camundaClient) {
    this.camundaClient = camundaClient;
  }

  public ModifyProcessInstanceCommandStep1 newModifyProcessInstanceCommand(
      final Long processInstanceKey) {
    return camundaClient.newModifyProcessInstanceCommand(processInstanceKey);
  }

  public void setVariablesInZeebe(final Long scopeKey, final Map<String, Object> variables) {
    camundaClient.newSetVariablesCommand(scopeKey).variables(variables).local(true).send().join();
  }

  public void sendModificationsToZeebe(
      final ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep2 stepCommand) {
    if (stepCommand != null) {
      stepCommand.send().join();
    }
  }
}
