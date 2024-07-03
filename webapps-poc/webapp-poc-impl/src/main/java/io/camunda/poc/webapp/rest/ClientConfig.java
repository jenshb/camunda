/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp.rest;

import io.camunda.poc.webapp.property.WebAppsPocProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientConfig {

  public boolean isEnterprise;
  public boolean isMultiTenancyEnabled;
  public boolean canLogout;
  public boolean isLoginDelegated;
  public String contextPath;
  public String baseName;

  // Cloud related properties for mixpanel events
  @Value("${CAMUNDA_WEBAPPS_CLOUD_ORGANIZATIONID:#{null}}")
  public String organizationId;

  @Value("${CAMUNDA_WEBAPPS_CLOUD_CLUSTERID:#{null}}")
  public String clusterId;

  @Value("${CAMUNDA_WEBAPPS_CLOUD_STAGE:#{null}}")
  public String stage;

  @Value("${CAMUNDA_WEBAPPS_CLOUD_MIXPANELTOKEN:#{null}}")
  public String mixpanelToken;

  @Value("${CAMUNDA_WEBAPPS_CLOUD_MIXPANELAPIHOST:#{null}}")
  public String mixpanelAPIHost;

  @Value("${CAMUNDA_WEBAPPS_IDENTITY_RESOURCE_PERMISSIONS_ENABLED:#{false}}")
  public boolean isResourcePermissionsEnabled;

  @Value("${CAMUNDA_WEBAPPS_IDENTITY_USER_ACCESS_RESTRICTIONS_ENABLED:#{true}}")
  public boolean isUserAccessRestrictionsEnabled;

  // @Autowired private TasklistProfileService profileService;
  @Autowired private WebAppsPocProperties webAppsPocProperties;
  @Autowired private ServletContext context;

  @PostConstruct
  public void init() {
    isEnterprise = webAppsPocProperties.isEnterprise();
    isMultiTenancyEnabled = webAppsPocProperties.getMultiTenancy().isEnabled();
    contextPath = context.getContextPath();
    baseName = context.getContextPath() + "/tasklist";
    // canLogout = profileService.currentProfileCanLogout();
    // isLoginDelegated = profileService.isLoginDelegated();
  }
}
