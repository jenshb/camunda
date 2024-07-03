/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp.property;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/** This class contains all project configuration parameters. */
@Component
@Configuration
@ConfigurationProperties(WebAppsPocProperties.PREFIX)
@PropertySource("classpath:tasklist-version.properties")
public class WebAppsPocProperties {

  public static final String PREFIX = "camunda.tasklist";
  public static final String ALPHA_RELEASES_SUFIX = "alpha";
  public static final long BATCH_OPERATION_MAX_SIZE_DEFAULT = 1_000_000L;
  public static final String ELASTIC_SEARCH = "elasticsearch";
  public static final String OPEN_SEARCH = "opensearch";
  public static String database = ELASTIC_SEARCH;
  private static final String UNKNOWN_VERSION = "unknown-version";

  private boolean importerEnabled = true;
  private boolean archiverEnabled = true;
  private boolean webappEnabled = true;

  private boolean persistentSessionsEnabled = false;
  private boolean csrfPreventionEnabled = true;
  private boolean fixUsernames = true;
  private String userId = "demo";
  private String displayName = "demo";
  private String password = "demo";
  private String operatorUserId = "act";
  private String operatorPassword = "act";
  private String operatorDisplayName = "act";
  private String readerUserId = "view";
  private String readerPassword = "view";
  private String readerDisplayName = "view";

  private List<String> roles = List.of("OWNER");

  /** Maximum size of batch operation. */
  private Long batchOperationMaxSize = BATCH_OPERATION_MAX_SIZE_DEFAULT;

  private boolean enterprise = false;

  @Value("${camunda.tasklist.internal.version.current}")
  private String version = UNKNOWN_VERSION;

  @NestedConfigurationProperty
  private SecurityProperties securityProperties = new SecurityProperties();

  @NestedConfigurationProperty private ImportProperties importer = new ImportProperties();

  @NestedConfigurationProperty private ArchiverProperties archiver = new ArchiverProperties();

  @NestedConfigurationProperty private ClientProperties client = new ClientProperties();

  @NestedConfigurationProperty private CloudProperties cloud = new CloudProperties();

  @NestedConfigurationProperty
  private FeatureFlagProperties featureFlag = new FeatureFlagProperties();

  @NestedConfigurationProperty
  private ClusterNodeProperties clusterNode = new ClusterNodeProperties();

  @NestedConfigurationProperty private Auth0Properties auth0 = new Auth0Properties();

  @NestedConfigurationProperty private IdentityProperties identity = new IdentityProperties();

  @NestedConfigurationProperty private BackupProperties backup = new BackupProperties();

  @NestedConfigurationProperty
  private MultiTenancyProperties multiTenancy = new MultiTenancyProperties();

  @NestedConfigurationProperty
  private WebAppsPocDocumentationProperties documentation = new WebAppsPocDocumentationProperties();

  public boolean isImporterEnabled() {
    return importerEnabled;
  }

  public void setImporterEnabled(final boolean importerEnabled) {
    this.importerEnabled = importerEnabled;
  }

  public boolean isArchiverEnabled() {
    return archiverEnabled;
  }

  public void setArchiverEnabled(final boolean archiverEnabled) {
    this.archiverEnabled = archiverEnabled;
  }

  public boolean isWebappEnabled() {
    return webappEnabled;
  }

  public void setWebappEnabled(final boolean webappEnabled) {
    this.webappEnabled = webappEnabled;
  }

  public Long getBatchOperationMaxSize() {
    return batchOperationMaxSize;
  }

  public void setBatchOperationMaxSize(final Long batchOperationMaxSize) {
    this.batchOperationMaxSize = batchOperationMaxSize;
  }

  public boolean isAlphaVersion() {
    return getVersion().toLowerCase().contains(WebAppsPocProperties.ALPHA_RELEASES_SUFIX);
  }

  public boolean isSelfManaged() {
    return getCloud().getClusterId() == null;
  }

  public boolean isCsrfPreventionEnabled() {
    return csrfPreventionEnabled;
  }

  public void setCsrfPreventionEnabled(final boolean csrfPreventionEnabled) {
    this.csrfPreventionEnabled = csrfPreventionEnabled;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public ImportProperties getImporter() {
    return importer;
  }

  public void setImporter(final ImportProperties importer) {
    this.importer = importer;
  }

  public ArchiverProperties getArchiver() {
    return archiver;
  }

  public void setArchiver(final ArchiverProperties archiver) {
    this.archiver = archiver;
  }

  public ClusterNodeProperties getClusterNode() {
    return clusterNode;
  }

  public void setClusterNode(final ClusterNodeProperties clusterNode) {
    this.clusterNode = clusterNode;
  }

  public boolean isEnterprise() {
    return enterprise;
  }

  public void setEnterprise(final boolean enterprise) {
    this.enterprise = enterprise;
  }

  public ClientProperties getClient() {
    return client;
  }

  public void setClient(final ClientProperties client) {
    this.client = client;
  }

  public boolean isPersistentSessionsEnabled() {
    return persistentSessionsEnabled;
  }

  public WebAppsPocProperties setPersistentSessionsEnabled(
      final boolean persistentSessionsEnabled) {
    this.persistentSessionsEnabled = persistentSessionsEnabled;
    return this;
  }

  public Auth0Properties getAuth0() {
    return auth0;
  }

  public WebAppsPocProperties setAuth0(final Auth0Properties auth0) {
    this.auth0 = auth0;
    return this;
  }

  public IdentityProperties getIdentity() {
    return identity;
  }

  public void setIdentity(final IdentityProperties identity) {
    this.identity = identity;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(final List<String> roles) {
    this.roles = roles;
  }

  public CloudProperties getCloud() {
    return cloud;
  }

  public void setCloud(final CloudProperties cloud) {
    this.cloud = cloud;
  }

  public BackupProperties getBackup() {
    return backup;
  }

  public WebAppsPocProperties setBackup(final BackupProperties backup) {
    this.backup = backup;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public WebAppsPocProperties setVersion(final String version) {
    this.version = version;
    return this;
  }

  public boolean isFixUsernames() {
    return fixUsernames;
  }

  public void setFixUsernames(final boolean fixUsernames) {
    this.fixUsernames = fixUsernames;
  }

  public SecurityProperties getSecurityProperties() {
    return securityProperties;
  }

  public WebAppsPocProperties setSecurityProperties(final SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
    return this;
  }

  public FeatureFlagProperties getFeatureFlag() {
    return featureFlag;
  }

  public WebAppsPocProperties setFeatureFlag(final FeatureFlagProperties featureFlag) {
    this.featureFlag = featureFlag;
    return this;
  }

  public String getOperatorUserId() {
    return operatorUserId;
  }

  public WebAppsPocProperties setOperatorUserId(final String operatorUserId) {
    this.operatorUserId = operatorUserId;
    return this;
  }

  public String getOperatorPassword() {
    return operatorPassword;
  }

  public WebAppsPocProperties setOperatorPassword(final String operatorPassword) {
    this.operatorPassword = operatorPassword;
    return this;
  }

  public String getOperatorDisplayName() {
    return operatorDisplayName;
  }

  public WebAppsPocProperties setOperatorDisplayName(final String operatorDisplayName) {
    this.operatorDisplayName = operatorDisplayName;
    return this;
  }

  public String getReaderUserId() {
    return readerUserId;
  }

  public WebAppsPocProperties setReaderUserId(final String readerUserId) {
    this.readerUserId = readerUserId;
    return this;
  }

  public String getReaderPassword() {
    return readerPassword;
  }

  public WebAppsPocProperties setReaderPassword(final String readerPassword) {
    this.readerPassword = readerPassword;
    return this;
  }

  public String getReaderDisplayName() {
    return readerDisplayName;
  }

  public WebAppsPocProperties setReaderDisplayName(final String readerDisplayName) {
    this.readerDisplayName = readerDisplayName;
    return this;
  }

  public static String getDatabase() {
    return database;
  }

  public WebAppsPocProperties setDatabase(final String database) {
    this.database = database;
    return this;
  }

  public MultiTenancyProperties getMultiTenancy() {
    return multiTenancy;
  }

  public WebAppsPocProperties setMultiTenancy(final MultiTenancyProperties multiTenancy) {
    this.multiTenancy = multiTenancy;
    return this;
  }

  public WebAppsPocDocumentationProperties getDocumentation() {
    return documentation;
  }

  public void setDocumentation(final WebAppsPocDocumentationProperties documentation) {
    this.documentation = documentation;
  }

  public String getDatabaseType() {
    return database;
  }
}
