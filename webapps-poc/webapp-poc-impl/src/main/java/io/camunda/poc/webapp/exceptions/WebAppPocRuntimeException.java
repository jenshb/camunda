/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp.exceptions;

public class WebAppPocRuntimeException extends RuntimeException {

  public WebAppPocRuntimeException() {}

  public WebAppPocRuntimeException(final String message) {
    super(message);
  }

  public WebAppPocRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public WebAppPocRuntimeException(final Throwable cause) {
    super(cause);
  }
}
