/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp.security.oauth;

import io.camunda.identity.sdk.Identity;
import io.camunda.poc.webapp.property.WebAppsPocProperties;
import io.camunda.poc.webapp.security.tenant.TenantAwareAuthentication;
import io.camunda.poc.webapp.security.tenant.WebAppsPocTenant;
import io.camunda.poc.webapp.util.SpringContextHolder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class IdentityTenantAwareJwtAuthenticationToken extends JwtAuthenticationToken
    implements TenantAwareAuthentication {

  private static final long serialVersionUID = 1L;

  private List<WebAppsPocTenant> tenants = Collections.emptyList();

  public IdentityTenantAwareJwtAuthenticationToken(
      final Jwt jwt, final Collection<? extends GrantedAuthority> authorities, final String name) {
    super(jwt, authorities, name);
  }

  @Override
  public List<WebAppsPocTenant> getTenants() {
    if (CollectionUtils.isEmpty(tenants) && isMultiTenancyEnabled()) {
      tenants = retrieveTenants();
    }
    return tenants;
  }

  private List<WebAppsPocTenant> retrieveTenants() {
    try {
      final var token = getToken().getTokenValue();
      final var identityTenants = getIdentity().tenants().forToken(token);
      if (CollectionUtils.isEmpty(identityTenants)) {
        return Collections.emptyList();
      } else {
        return identityTenants.stream()
            .map((t) -> new WebAppsPocTenant(t.getTenantId(), t.getName()))
            .sorted(TENANT_NAMES_COMPARATOR)
            .toList();
      }
    } catch (final Exception e) {
      throw new InsufficientAuthenticationException(e.getMessage(), e);
    }
  }

  private Identity getIdentity() {
    return SpringContextHolder.getBean(Identity.class);
  }

  private boolean isMultiTenancyEnabled() {
    return SpringContextHolder.getBean(WebAppsPocProperties.class).getMultiTenancy().isEnabled();
  }
}
