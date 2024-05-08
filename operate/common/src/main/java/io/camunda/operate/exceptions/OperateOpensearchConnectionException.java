/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class OperateOpensearchConnectionException extends OperateRuntimeException {

  public OperateOpensearchConnectionException() {}

  public OperateOpensearchConnectionException(String message) {
    super(message);
  }

  public OperateOpensearchConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public OperateOpensearchConnectionException(Throwable cause) {
    super(cause);
  }
}