/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.camunda.optimize.dto.optimize.OptimizeDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldNameConstants
public class IndexableEventBasedProcessDto implements OptimizeDto {
  private String id;
  private String name;
  private String xml;
  private OffsetDateTime lastModified;
  private String lastModifier;
  private List<IndexableEventMappingDto> mappings;

  public static IndexableEventBasedProcessDto fromEventBasedProcessDto(EventBasedProcessDto eventBasedProcessDto) {
    return IndexableEventBasedProcessDto.builder()
      .id(eventBasedProcessDto.getId())
      .name(eventBasedProcessDto.getName())
      .xml(eventBasedProcessDto.getXml())
      .lastModified(eventBasedProcessDto.getLastModified())
      .lastModifier(eventBasedProcessDto.getLastModifier())
      .mappings(Optional.ofNullable(eventBasedProcessDto.getMappings())
                  .map(mappings -> mappings.keySet()
                    .stream()
                    .map(flowNodeId -> createIndexableEventMapping(flowNodeId, eventBasedProcessDto.getMappings().get(flowNodeId)))
                    .collect(Collectors.toList()))
                  .orElse(null))
      .build();

  }

  public EventBasedProcessDto toEventBasedProcessDto() {
    return EventBasedProcessDto.builder()
      .id(this.id)
      .name(this.name)
      .xml(this.xml)
      .lastModified(this.lastModified)
      .lastModifier(this.lastModifier)
      .mappings(Optional.ofNullable(this.mappings)
        .map(mappings -> mappings.stream()
          .collect(Collectors.toMap(
            IndexableEventMappingDto::getFlowNodeId,
            mapping -> EventMappingDto.builder()
              .start(mapping.getStart())
              .end(mapping.getEnd()).build()
          ))).orElse(null))
      .build();
  }

  private static IndexableEventMappingDto createIndexableEventMapping(String flowNodeId, EventMappingDto eventMappingDto) {
    return IndexableEventMappingDto.builder()
      .flowNodeId(flowNodeId)
      .start(eventMappingDto.getStart())
      .end(eventMappingDto.getEnd())
      .build();
  }

}
