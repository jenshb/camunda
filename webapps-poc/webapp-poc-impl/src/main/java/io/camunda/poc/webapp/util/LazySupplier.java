/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp.util;

import io.camunda.poc.webapp.exceptions.WebAppPocRuntimeException;
import java.util.function.Supplier;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

public final class LazySupplier<T> implements Supplier<T> {
  private final LazyInitializer<T> lazyInitializer;

  private LazySupplier(final LazyInitializer<T> lazyInitializer) {
    this.lazyInitializer = lazyInitializer;
  }

  public static <T> LazySupplier<T> of(final Supplier<T> supplier) {
    return new LazySupplier<>(LazyInitializer.<T>builder().setInitializer(supplier::get).get());
  }

  @Override
  public T get() {
    try {
      return lazyInitializer.get();
    } catch (final ConcurrentException e) {
      throw new WebAppPocRuntimeException(e);
    }
  }
}
