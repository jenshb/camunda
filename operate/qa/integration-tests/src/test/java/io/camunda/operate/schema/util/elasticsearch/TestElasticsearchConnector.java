/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.schema.util.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.conditions.ElasticsearchCondition;
import io.camunda.operate.connect.ElasticsearchClientProvider;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.util.ObservableConnector;
import io.camunda.search.connect.configuration.ConnectConfiguration;
import io.camunda.search.connect.es.ElasticsearchConnector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Conditional;

@Conditional(ElasticsearchCondition.class)
public class TestElasticsearchConnector extends ElasticsearchClientProvider
    implements ObservableConnector {

  private final List<Consumer<OperateTestHttpRequest>> requestListeners = new ArrayList<>();

  public TestElasticsearchConnector(
      final OperateProperties operateProperties, final ObjectMapper objectMapper) {
    super(operateProperties, objectMapper);
  }

  @Override
  protected ElasticsearchConnector createElasticsearchConnector(
      final ConnectConfiguration configuration, final ObjectMapper objectMapper) {
    return new TestHttpClientInterceptorESConnector(configuration, objectMapper, requestListeners);
  }

  @Override
  public void addRequestListener(final Consumer<OperateTestHttpRequest> listener) {
    requestListeners.add(listener);
  }

  @Override
  public void clearRequestListeners() {
    requestListeners.clear();
  }

  private static final class TestHttpClientInterceptorESConnector extends ElasticsearchConnector {

    private final List<Consumer<OperateTestHttpRequest>> requestListeners;

    public TestHttpClientInterceptorESConnector(
        final ConnectConfiguration configuration,
        final ObjectMapper objectMapper,
        final List<Consumer<OperateTestHttpRequest>> requestListeners) {
      super(configuration, objectMapper);
      this.requestListeners = requestListeners;
    }

    /**
     * Adds a request interceptor that a test case can plug in, so that we can assert requests made
     * to Elasticsearch
     */
    @Override
    protected HttpAsyncClientBuilder configureHttpClient(
        final HttpAsyncClientBuilder httpAsyncClientBuilder) {
      httpAsyncClientBuilder.addInterceptorFirst(
          new HttpRequestInterceptor() {

            @Override
            public void process(final HttpRequest request, final HttpContext context)
                throws HttpException, IOException {
              final RequestLine requestLine = request.getRequestLine();

              requestListeners.forEach(
                  listener ->
                      listener.accept(
                          new OperateTestHttpRequest() {

                            @Override
                            public String getUri() {
                              return requestLine.getUri();
                            }

                            @Override
                            public String getMethod() {
                              return requestLine.getMethod();
                            }
                          }));
            }
          });
      return super.configureHttpClient(httpAsyncClientBuilder);
    }
  }
}
