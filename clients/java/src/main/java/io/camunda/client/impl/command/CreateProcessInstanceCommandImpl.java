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
package io.camunda.client.impl.command;

import io.camunda.client.CamundaClientConfiguration;
import io.camunda.client.CredentialsProvider.StatusCode;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.JsonMapper;
import io.camunda.client.api.command.CommandWithTenantStep;
import io.camunda.client.api.command.CreateProcessInstanceCommandStep1;
import io.camunda.client.api.command.CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep2;
import io.camunda.client.api.command.CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep3;
import io.camunda.client.api.command.FinalCommandStep;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.impl.RetriableClientFutureImpl;
import io.camunda.client.impl.response.CreateProcessInstanceResponseImpl;
import io.camunda.zeebe.gateway.protocol.GatewayGrpc.GatewayStub;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceRequest.Builder;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ProcessInstanceCreationStartInstruction;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class CreateProcessInstanceCommandImpl
    extends CommandWithVariables<CreateProcessInstanceCommandImpl>
    implements CreateProcessInstanceCommandStep1,
        CreateProcessInstanceCommandStep2,
        CreateProcessInstanceCommandStep3 {

  private final GatewayStub asyncStub;
  private final Builder builder;
  private final Predicate<StatusCode> retryPredicate;
  private final JsonMapper jsonMapper;
  private Duration requestTimeout;

  public CreateProcessInstanceCommandImpl(
      final GatewayStub asyncStub,
      final JsonMapper jsonMapper,
      final CamundaClientConfiguration config,
      final Predicate<StatusCode> retryPredicate) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    requestTimeout = config.getDefaultRequestTimeout();
    this.retryPredicate = retryPredicate;
    this.jsonMapper = jsonMapper;
    builder = CreateProcessInstanceRequest.newBuilder();
    tenantId(config.getDefaultTenantId());
  }

  /**
   * A constructor that provides an instance with the <code><default></code> tenantId set.
   *
   * <p>From version 8.3.0, the java client supports multi-tenancy for this command, which requires
   * the <code>tenantId</code> property to be defined. This constructor is only intended for
   * backwards compatibility in tests.
   *
   * @deprecated since 8.3.0, use {@link
   *     CreateProcessInstanceCommandImpl#CreateProcessInstanceCommandImpl(GatewayStub asyncStub,
   *     JsonMapper jsonMapper, CamundaClientConfiguration config, Predicate retryPredicate)}
   */
  public CreateProcessInstanceCommandImpl(
      final GatewayStub asyncStub,
      final JsonMapper jsonMapper,
      final Duration requestTimeout,
      final Predicate<StatusCode> retryPredicate) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
    this.jsonMapper = jsonMapper;
    builder = CreateProcessInstanceRequest.newBuilder();
    tenantId(CommandWithTenantStep.DEFAULT_TENANT_IDENTIFIER);
  }

  @Override
  protected CreateProcessInstanceCommandImpl setVariablesInternal(final String variables) {
    builder.setVariables(variables);
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 startBeforeElement(final String elementId) {
    builder.addStartInstructions(
        ProcessInstanceCreationStartInstruction.newBuilder().setElementId(elementId).build());

    return this;
  }

  @Override
  public CreateProcessInstanceWithResultCommandStep1 withResult() {
    return new CreateProcessInstanceWithResultCommandImpl(
        jsonMapper, asyncStub, builder, retryPredicate, requestTimeout);
  }

  @Override
  public CreateProcessInstanceCommandStep2 bpmnProcessId(final String id) {
    builder.setBpmnProcessId(id);
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 processDefinitionKey(final long processDefinitionKey) {
    builder.setProcessDefinitionKey(processDefinitionKey);
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 version(final int version) {
    builder.setVersion(version);
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 latestVersion() {
    return version(LATEST_VERSION);
  }

  @Override
  public FinalCommandStep<ProcessInstanceEvent> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public CamundaFuture<ProcessInstanceEvent> send() {
    final CreateProcessInstanceRequest request = builder.build();

    final RetriableClientFutureImpl<ProcessInstanceEvent, CreateProcessInstanceResponse> future =
        new RetriableClientFutureImpl<>(
            CreateProcessInstanceResponseImpl::new,
            retryPredicate,
            streamObserver -> send(request, streamObserver));

    send(request, future);
    return future;
  }

  @Override
  public CreateProcessInstanceCommandStep3 tenantId(final String tenantId) {
    builder.setTenantId(tenantId);
    return this;
  }

  private void send(
      final CreateProcessInstanceRequest request,
      final StreamObserver<GatewayOuterClass.CreateProcessInstanceResponse> future) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .createProcessInstance(request, future);
  }
}
