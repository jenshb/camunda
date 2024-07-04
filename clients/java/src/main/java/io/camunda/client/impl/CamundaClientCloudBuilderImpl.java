/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.client.impl;

import static io.camunda.client.impl.BuilderUtils.appendProperty;
import static io.camunda.client.impl.command.ArgumentUtil.ensureNotNull;

import io.camunda.client.CamundaClient;
import io.camunda.client.CamundaClientBuilder;
import io.camunda.client.CamundaClientCloudBuilderStep1;
import io.camunda.client.CamundaClientCloudBuilderStep1.CamundaClientCloudBuilderStep2;
import io.camunda.client.CamundaClientCloudBuilderStep1.CamundaClientCloudBuilderStep2.CamundaClientCloudBuilderStep3;
import io.camunda.client.CamundaClientCloudBuilderStep1.CamundaClientCloudBuilderStep2.CamundaClientCloudBuilderStep3.CamundaClientCloudBuilderStep4;
import io.camunda.client.ClientProperties;
import io.camunda.client.CredentialsProvider;
import io.camunda.client.api.ExperimentalApi;
import io.camunda.client.api.JsonMapper;
import io.camunda.client.impl.oauth.OAuthCredentialsProviderBuilder;
import io.grpc.ClientInterceptor;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

public class CamundaClientCloudBuilderImpl
    implements CamundaClientCloudBuilderStep1,
        CamundaClientCloudBuilderStep2,
        CamundaClientCloudBuilderStep3,
        CamundaClientCloudBuilderStep4 {

  private static final String BASE_ADDRESS = "zeebe.camunda.io";
  private static final String BASE_AUTH_URL = "https://login.cloud.camunda.io/oauth/token";

  private static final String DEFAULT_REGION = "bru-2";

  private final CamundaClientBuilderImpl innerBuilder = new CamundaClientBuilderImpl();

  private String clusterId;
  private String clientId;
  private String clientSecret;
  private String region = DEFAULT_REGION;

  @Override
  public CamundaClientCloudBuilderStep2 withClusterId(final String clusterId) {
    this.clusterId = clusterId;
    return this;
  }

  @Override
  public CamundaClientCloudBuilderStep3 withClientId(final String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Override
  public CamundaClientCloudBuilderStep4 withClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  @Override
  public CamundaClientCloudBuilderStep4 withRegion(final String region) {
    this.region = region;
    return this;
  }

  @Override
  public CamundaClientCloudBuilderStep4 withProperties(final Properties properties) {
    if (properties.containsKey(ClientProperties.CLOUD_CLUSTER_ID)) {
      withClusterId(properties.getProperty(ClientProperties.CLOUD_CLUSTER_ID));
    }
    if (properties.containsKey(ClientProperties.CLOUD_CLIENT_ID)) {
      withClientId(properties.getProperty(ClientProperties.CLOUD_CLIENT_ID));
    }
    if (properties.containsKey(ClientProperties.CLOUD_CLIENT_SECRET)) {
      withClientSecret(properties.getProperty(ClientProperties.CLOUD_CLIENT_SECRET));
    }
    if (properties.containsKey(ClientProperties.CLOUD_REGION)) {
      withRegion(properties.getProperty(ClientProperties.CLOUD_REGION));
    }
    if (properties.containsKey(ClientProperties.STREAM_ENABLED)) {
      defaultJobWorkerStreamEnabled(
          Boolean.parseBoolean(properties.getProperty(ClientProperties.STREAM_ENABLED)));
    }
    innerBuilder.withProperties(properties);

    // todo(#14106): allow default tenant id setting for cloud client
    innerBuilder.defaultTenantId("");
    innerBuilder.defaultJobWorkerTenantIds(Collections.emptyList());

    return this;
  }

  @Override
  public CamundaClientBuilder applyEnvironmentVariableOverrides(
      final boolean applyEnvironmentVariableOverrides) {
    innerBuilder.applyEnvironmentVariableOverrides(applyEnvironmentVariableOverrides);
    return this;
  }

  @Override
  public CamundaClientBuilder gatewayAddress(final String gatewayAddress) {
    innerBuilder.gatewayAddress(gatewayAddress);
    return this;
  }

  @Override
  public CamundaClientBuilder restAddress(final URI restAddress) {
    innerBuilder.restAddress(restAddress);
    return this;
  }

  @Override
  public CamundaClientBuilder grpcAddress(final URI grpcAddress) {
    innerBuilder.grpcAddress(grpcAddress);
    return this;
  }

  @Override
  @ExperimentalApi("https://github.com/camunda/zeebe/issues/14106")
  public CamundaClientBuilder defaultTenantId(final String tenantId) {
    Loggers.LOGGER.debug(
        "Multi-tenancy in Camunda 8 SaaS will be supported with https://github.com/camunda/zeebe/issues/14106.");
    return this;
  }

  @Override
  @ExperimentalApi("https://github.com/camunda/zeebe/issues/14106")
  public CamundaClientBuilder defaultJobWorkerTenantIds(final List<String> tenantIds) {
    Loggers.LOGGER.debug(
        "Multi-tenancy in Camunda 8 SaaS will be supported with https://github.com/camunda/zeebe/issues/14106.");
    return this;
  }

  @Override
  public CamundaClientBuilder defaultJobWorkerMaxJobsActive(final int maxJobsActive) {
    innerBuilder.defaultJobWorkerMaxJobsActive(maxJobsActive);
    return this;
  }

  @Override
  public CamundaClientBuilder numJobWorkerExecutionThreads(final int numThreads) {
    innerBuilder.numJobWorkerExecutionThreads(numThreads);
    return this;
  }

  @Override
  public CamundaClientBuilder jobWorkerExecutor(
      final ScheduledExecutorService executor, final boolean takeOwnership) {
    innerBuilder.jobWorkerExecutor(executor, takeOwnership);
    return this;
  }

  @Override
  public CamundaClientBuilder defaultJobWorkerName(final String workerName) {
    innerBuilder.defaultJobWorkerName(workerName);
    return this;
  }

  @Override
  public CamundaClientBuilder defaultJobTimeout(final Duration timeout) {
    innerBuilder.defaultJobTimeout(timeout);
    return this;
  }

  @Override
  public CamundaClientBuilder defaultJobPollInterval(final Duration pollInterval) {
    innerBuilder.defaultJobPollInterval(pollInterval);
    return this;
  }

  @Override
  public CamundaClientBuilder defaultMessageTimeToLive(final Duration timeToLive) {
    innerBuilder.defaultMessageTimeToLive(timeToLive);
    return this;
  }

  @Override
  public CamundaClientBuilder defaultRequestTimeout(final Duration requestTimeout) {
    innerBuilder.defaultRequestTimeout(requestTimeout);
    return this;
  }

  @Override
  public CamundaClientBuilder usePlaintext() {
    innerBuilder.usePlaintext();
    return this;
  }

  @Override
  public CamundaClientBuilder caCertificatePath(final String certificatePath) {
    innerBuilder.caCertificatePath(certificatePath);
    return this;
  }

  @Override
  public CamundaClientBuilder credentialsProvider(final CredentialsProvider credentialsProvider) {
    innerBuilder.credentialsProvider(credentialsProvider);
    return this;
  }

  @Override
  public CamundaClientBuilder keepAlive(final Duration keepAlive) {
    innerBuilder.keepAlive(keepAlive);
    return this;
  }

  @Override
  public CamundaClientBuilder withInterceptors(final ClientInterceptor... interceptor) {
    innerBuilder.withInterceptors(interceptor);
    return this;
  }

  @Override
  public CamundaClientBuilder withJsonMapper(final JsonMapper jsonMapper) {
    innerBuilder.withJsonMapper(jsonMapper);
    return this;
  }

  @Override
  public CamundaClientBuilder overrideAuthority(final String authority) {
    innerBuilder.overrideAuthority(authority);
    return this;
  }

  @Override
  public CamundaClientBuilder maxMessageSize(final int maxMessageSize) {
    innerBuilder.maxMessageSize(maxMessageSize);
    return this;
  }

  @Override
  public CamundaClientBuilder defaultJobWorkerStreamEnabled(final boolean streamEnabled) {
    innerBuilder.defaultJobWorkerStreamEnabled(streamEnabled);
    return this;
  }

  @Override
  public CamundaClientBuilder useDefaultRetryPolicy(final boolean useDefaultRetryPolicy) {
    innerBuilder.useDefaultRetryPolicy(useDefaultRetryPolicy);
    return this;
  }

  @Override
  public CamundaClientBuilder preferRestOverGrpc(final boolean preferRestOverGrpc) {
    innerBuilder.preferRestOverGrpc(preferRestOverGrpc);
    return this;
  }

  @Override
  public CamundaClient build() {
    innerBuilder.grpcAddress(determineGrpcAddress());
    innerBuilder.restAddress(determineRestAddress());
    innerBuilder.credentialsProvider(determineCredentialsProvider());
    return innerBuilder.build();
  }

  private URI determineRestAddress() {
    if (isNeedToSetCloudRestAddress()) {
      ensureNotNull("cluster id", clusterId);
      final String cloudRestAddress =
          String.format("https://%s.zeebe.%s:443/%s", region, BASE_ADDRESS, clusterId);
      return getURIFromString(cloudRestAddress);
    } else {
      Loggers.LOGGER.debug(
          "Expected to use 'cluster id' to set REST API address in the client cloud builder, "
              + "but overwriting with explicitly defined REST API address: {}.",
          innerBuilder.getRestAddress());
      return innerBuilder.getRestAddress();
    }
  }

  private URI determineGrpcAddress() {
    if (isNeedToSetCloudGrpcAddress() && isNeedToSetCloudGatewayAddress()) {
      ensureNotNull("cluster id", clusterId);
      final String cloudGrpcAddress =
          String.format("https://%s.%s.%s:443", clusterId, region, BASE_ADDRESS);
      return getURIFromString(cloudGrpcAddress);
    } else {
      if (!isNeedToSetCloudGrpcAddress()) {
        Loggers.LOGGER.debug(
            "Expected to use 'cluster id' to set gateway address in the client cloud builder, "
                + "but overwriting with explicitly defined gateway address: {}.",
            innerBuilder.getGrpcAddress());
        return innerBuilder.getGrpcAddress();
      }

      Loggers.LOGGER.debug(
          "Expected to use 'cluster id' to set gateway address in the client cloud builder, "
              + "but overwriting with explicitly defined gateway address: {}.",
          innerBuilder.getGatewayAddress());
      return getURIFromString("https://" + innerBuilder.getGatewayAddress());
    }
  }

  private CredentialsProvider determineCredentialsProvider() {
    final CredentialsProvider provider = innerBuilder.getCredentialsProvider();
    if (provider == null) {
      ensureNotNull("cluster id", clusterId);
      ensureNotNull("client id", clientId);
      ensureNotNull("client secret", clientSecret);
      final OAuthCredentialsProviderBuilder builder = new OAuthCredentialsProviderBuilder();

      if (innerBuilder.isPlaintextConnectionEnabled()) {
        Loggers.LOGGER.debug("Expected setting 'usePlaintext' to be 'false', but found 'true'.");
      }
      return builder
          .audience(String.format("%s.%s.%s", clusterId, region, BASE_ADDRESS))
          .clientId(clientId)
          .clientSecret(clientSecret)
          .authorizationServerUrl(BASE_AUTH_URL)
          .build();
    } else {
      Loggers.LOGGER.debug(
          "Expected to use 'cluster id', 'client id' and 'client secret' to set credentials provider in the client cloud builder, "
              + "but overwriting with explicitly defined credentials provider.");
      return provider;
    }
  }

  private boolean isNeedToSetCloudGrpcAddress() {
    return innerBuilder.getGrpcAddress() == null
        || Objects.equals(
            innerBuilder.getGrpcAddress(), CamundaClientBuilderImpl.DEFAULT_GRPC_ADDRESS);
  }

  private boolean isNeedToSetCloudGatewayAddress() {
    return innerBuilder.getGatewayAddress() == null
        || Objects.equals(
            innerBuilder.getGatewayAddress(), CamundaClientBuilderImpl.DEFAULT_GATEWAY_ADDRESS);
  }

  private boolean isNeedToSetCloudRestAddress() {
    return innerBuilder.getRestAddress() == null
        || Objects.equals(
            innerBuilder.getRestAddress(), CamundaClientBuilderImpl.DEFAULT_REST_ADDRESS);
  }

  private URI getURIFromString(final String uri) {
    try {
      return new URI(uri);
    } catch (final URISyntaxException e) {
      throw new RuntimeException("Failed to parse URI string", e);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(innerBuilder.toString());
    appendProperty(sb, "clusterId", clusterId);
    appendProperty(sb, "clientId", clientId);
    appendProperty(sb, "region", region);
    return sb.toString();
  }
}