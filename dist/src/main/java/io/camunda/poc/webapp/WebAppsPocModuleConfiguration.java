/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp;

import graphql.kickstart.autoconfigure.annotations.GraphQLAnnotationsAutoConfiguration;
import io.camunda.poc.webapp.management.WebappManagementModuleConfiguration;
import io.camunda.poc.webapp.security.WebappSecurityModuleConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Entry point for the Tasklist modules by using the the {@link
 * io.camunda.application.Profile#TASKLIST} profile, so that the appropriate Tasklist application
 * properties are applied.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = "io.camunda.poc.webapp",
    excludeFilters = {
      @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.camunda\\.poc\\.webapp\\..*"),
    },
    // use fully qualified names as bean name, as we have classes with same names for different
    // versions of importer
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableAutoConfiguration(
    exclude = {
      ElasticsearchClientAutoConfiguration.class,
      GraphQLAnnotationsAutoConfiguration.class
    })
@Profile("webapps-poc")
public class WebAppsPocModuleConfiguration {

  public WebAppsPocModuleConfiguration() {}

  @Configuration(proxyBeanMethods = false)
  @Import({WebappSecurityModuleConfiguration.class})
  @Profile("!operate")
  public static class WebAppsSecurityModulesConfiguration {}

  @Configuration(proxyBeanMethods = false)
  @Import(WebappManagementModuleConfiguration.class)
  @Profile("!operate")
  public static class WebAppsManagementModulesConfiguration {}
}
