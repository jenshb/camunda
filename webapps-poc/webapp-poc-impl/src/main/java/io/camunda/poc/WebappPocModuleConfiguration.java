/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@Configuration
@ComponentScan(
    basePackages = "io.camunda.poc.webapp",
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.poc\\.webapp\\.security\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.poc\\.webapp\\.management\\..*")
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@ConditionalOnProperty(
    name = "camunda.poc.webappEnabled",
    havingValue = "true",
    matchIfMissing = true)
public class WebappPocModuleConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebappPocModuleConfiguration.class);

  @PostConstruct
  public void logModule() {
    LOGGER.info("Starting module: webapp");
  }
}
