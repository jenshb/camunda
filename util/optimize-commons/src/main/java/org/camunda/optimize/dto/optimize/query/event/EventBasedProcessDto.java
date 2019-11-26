/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.camunda.optimize.dto.optimize.OptimizeDto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
public class EventBasedProcessDto implements OptimizeDto {
  @EqualsAndHashCode.Include
  private String id;
  @NotBlank
  private String name;

  private String lastModifier;

  private OffsetDateTime lastModified;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String xml;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Valid
  private Map<String, EventMappingDto> mappings;

}
