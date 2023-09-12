/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.webapp.security.identity;

import io.camunda.identity.sdk.Identity;
import io.camunda.operate.property.OperateProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"sso-auth"})
@SpringBootTest(
    classes = {
        IdentityConfigurer.class,
        OperateProperties.class},
    properties = {
        OperateProperties.PREFIX + ".identity.resourcePermissionsEnabled = true"
    }
)
public class SSOIdentityCreatedTest {

  @Autowired
  @Qualifier("saasIdentity")
  private Identity identity;

  @Autowired
  private PermissionsService permissionsService;

  @Test
  public void testIdentityIsCreated() {
    assertThat(identity).isNotNull();
    assertThat(permissionsService).isNotNull();
  }

}
