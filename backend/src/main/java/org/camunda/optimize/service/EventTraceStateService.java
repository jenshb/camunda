/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

package org.camunda.optimize.service;

import lombok.AllArgsConstructor;
import org.camunda.optimize.dto.optimize.query.event.EventDto;
import org.camunda.optimize.dto.optimize.query.event.EventSequenceCountDto;
import org.camunda.optimize.dto.optimize.query.event.EventTraceStateDto;
import org.camunda.optimize.dto.optimize.query.event.EventTypeDto;
import org.camunda.optimize.dto.optimize.query.event.TracedEventDto;
import org.camunda.optimize.service.es.reader.EventSequenceCountReader;
import org.camunda.optimize.service.es.reader.EventTraceStateReader;
import org.camunda.optimize.service.es.writer.EventSequenceCountWriter;
import org.camunda.optimize.service.es.writer.EventTraceStateWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public class EventTraceStateService {
  private final EventTraceStateWriter eventTraceStateWriter;
  private final EventTraceStateReader eventTraceStateReader;
  private final EventSequenceCountWriter eventSequenceCountWriter;
  private final EventSequenceCountReader eventSequenceCountReader;

  public List<EventSequenceCountDto> getAllSequenceCounts() {
    return eventSequenceCountReader.getAllSequenceCounts();
  }

  public List<EventTraceStateDto> getTracesWithMaxResultSize(final int maxResultsSize) {
    return eventTraceStateReader.getTracesWithMaxResultSize(maxResultsSize);
  }

  public void updateTracesAndCountsForEvents(final List<EventDto> eventsToProcess) {
    Map<String, EventTraceStateDto> eventTraceStatesForUpdate = getEventTraceStatesForIds(
      eventsToProcess.stream().map(EventDto::getTraceId).distinct().collect(Collectors.toList())
    ).stream().collect(Collectors.toMap(EventTraceStateDto::getTraceId, Function.identity()));
    Map<String, EventTraceStateDto> eventTraceStatesToCreate = new HashMap<>();
    Map<String, EventSequenceCountDto> sequenceAdjustmentsRequired = new HashMap<>();

    for (EventDto event : eventsToProcess) {
      TracedEventDto tracedEventToAdd = TracedEventDto.fromEventDto(event);
      EventTraceStateDto currentTraceState = eventTraceStatesForUpdate.get(event.getTraceId());
      if (currentTraceState == null) {
        addOrUpdateNewTraceState(eventTraceStatesToCreate, event, tracedEventToAdd);
      } else {
        List<TracedEventDto> eventTrace = currentTraceState.getEventTrace();
        TracedEventDto tracedEventToRemove = getExistingTracedEventToBeReplaced(eventTrace, event);

        // We do nothing if the new event is a duplicate as the trace state will be unaffected
        if (!tracedEventToAdd.equals(tracedEventToRemove)) {
          if (tracedEventToRemove != null) {
            removeExistingEventFromTraceAndRecordAdjustments(
              eventTrace,
              tracedEventToRemove,
              sequenceAdjustmentsRequired
            );
          }
          addEventToTraceAndRecordAdjustments(eventTrace, tracedEventToAdd, sequenceAdjustmentsRequired);
        }
      }
    }

    // we merge the new and updated traces before doing a batch upsert
    eventTraceStatesToCreate.values().stream()
      .peek(traceState -> {
        final List<TracedEventDto> eventTrace = traceState.getEventTrace();
        sortTracedEvents(eventTrace);
      })
      .forEach(eventTrace -> addAdjustmentsForNewTraces(eventTrace, sequenceAdjustmentsRequired));

    eventTraceStatesForUpdate.putAll(eventTraceStatesToCreate);
    upsertEventStateTraces(new ArrayList<>(eventTraceStatesForUpdate.values()));

    // We filter out sequences that have a net 0 effect so don't need to be written
    final List<EventSequenceCountDto> adjustmentsToWrite = sequenceAdjustmentsRequired.keySet().stream()
      .filter(adjustment -> sequenceAdjustmentsRequired.get(adjustment).getCount() != 0L)
      .map(sequenceAdjustmentsRequired::get)
      .collect(Collectors.toList());
    updateEventSequenceWithAdjustments(adjustmentsToWrite);
  }

  private void sortTracedEvents(final List<TracedEventDto> eventTrace) {
    eventTrace.sort(Comparator.comparing(TracedEventDto::getTimestamp)
                    .thenComparing(compareOrderCounters())
                      .thenComparing(compareExistingSequenceCounts()));
  }

  private Comparator<TracedEventDto> compareOrderCounters() {
    return (tracedEventA, tracedEventB) -> {
      if (tracedEventA.getOrderCounter() != null && tracedEventB.getOrderCounter() != null) {
        return tracedEventA.getOrderCounter().compareTo(tracedEventB.getOrderCounter());
      }
      return 0;
    };
  }

  private Comparator<TracedEventDto> compareExistingSequenceCounts() {
    return (tracedEventA, tracedEventB) -> {
      final EventTypeDto eventATypeDto = toEventType(tracedEventA);
      final EventTypeDto eventBTypeDto = toEventType(tracedEventB);
      final List<EventSequenceCountDto> eventSequencesContainingEventTypes =
        eventSequenceCountReader.getEventSequencesContainingBothEventTypes(eventATypeDto, eventBTypeDto);
      if (eventSequencesContainingEventTypes.isEmpty()) {
        return 0;
      }
      final EventSequenceCountDto highestFrequencySequence = eventSequencesContainingEventTypes
        .stream()
        .max(Comparator.comparing(EventSequenceCountDto::getCount)).get();
      if (highestFrequencySequence.getSourceEvent().equals(eventATypeDto)) {
        return -1;
      } else {
        return 1;
      }
    };
  }

  private EventTypeDto toEventType(TracedEventDto tracedEventDto) {
    return EventTypeDto.builder()
      .eventName(tracedEventDto.getEventName())
      .source(tracedEventDto.getSource())
      .group(tracedEventDto.getGroup())
      .build();
  }

  private List<EventTraceStateDto> getEventTraceStatesForIds(List<String> traceIds) {
    return eventTraceStateReader.getEventTraceStateForTraceIds(traceIds);
  }

  private void upsertEventStateTraces(final List<EventTraceStateDto> eventTraceStateDtos) {
    eventTraceStateWriter.upsertEventTraceStates(eventTraceStateDtos);
  }

  private void updateEventSequenceWithAdjustments(final List<EventSequenceCountDto> adjustmentsToWrite) {
    eventSequenceCountWriter.updateEventSequenceCountsWithAdjustments(adjustmentsToWrite);
  }

  private TracedEventDto getExistingTracedEventToBeReplaced(final List<TracedEventDto> eventTrace,
                                                            final EventDto newEventDto) {
    return eventTrace.stream()
      .filter(tracedEvent -> Objects.equals(tracedEvent.getEventId(), newEventDto.getId())
        && Objects.equals(tracedEvent.getGroup(), newEventDto.getGroup())
        && Objects.equals(tracedEvent.getSource(), newEventDto.getSource())
        && Objects.equals(tracedEvent.getEventName(), newEventDto.getEventName()))
      .findAny()
      .orElse(null);
  }

  private void addOrUpdateNewTraceState(final Map<String, EventTraceStateDto> eventTraceStatesToCreate,
                                        final EventDto event, final TracedEventDto tracedEventToAdd) {
    Optional<EventTraceStateDto> existingTraceStateToAdd =
      Optional.ofNullable(eventTraceStatesToCreate.get(event.getTraceId()));

    // we might already have seen a new trace ID in this batch of events - this keeps linked events to a single trace
    if (existingTraceStateToAdd.isPresent()) {
      existingTraceStateToAdd.get().getEventTrace().add(tracedEventToAdd);
    } else {
      EventTraceStateDto newTraceStateDto = EventTraceStateDto.builder()
        .traceId(event.getTraceId())
        .eventTrace(new ArrayList<>(Collections.singletonList(tracedEventToAdd)))
        .build();
      eventTraceStatesToCreate.put(newTraceStateDto.getTraceId(), newTraceStateDto);
    }
  }

  private void addAdjustmentsForNewTraces(final EventTraceStateDto eventTraceStateToCreate,
                                          final Map<String, EventSequenceCountDto> adjustmentsRequired) {
    List<TracedEventDto> tracedEvents = eventTraceStateToCreate.getEventTrace();
    for (TracedEventDto event : tracedEvents) {
      int eventIndex = tracedEvents.indexOf(event);
      if (eventIndex == tracedEvents.size() - 1) {
        incrementSequenceAdjustment(createAdjustment(event, null), adjustmentsRequired);
      } else {
        incrementSequenceAdjustment(createAdjustment(event, tracedEvents.get(eventIndex + 1)), adjustmentsRequired);
      }
    }
  }

  private void addEventToTraceAndRecordAdjustments(final List<TracedEventDto> eventTrace,
                                                   final TracedEventDto tracedEventToAdd,
                                                   final Map<String, EventSequenceCountDto> requiredCountAdjustments) {
    eventTrace.add(tracedEventToAdd);
    sortTracedEvents(eventTrace);
    int indexOfNewEvent = eventTrace.indexOf(tracedEventToAdd);
    TracedEventDto newPreviousEvent = (indexOfNewEvent == 0) ? null : eventTrace.get(indexOfNewEvent - 1);
    TracedEventDto newNextEvent = (indexOfNewEvent == eventTrace.size() - 1) ? null :
      eventTrace.get(indexOfNewEvent + 1);
    if (newPreviousEvent != null) {
      decrementSequenceAdjustment(createAdjustment(newPreviousEvent, newNextEvent), requiredCountAdjustments);
      incrementSequenceAdjustment(createAdjustment(newPreviousEvent, tracedEventToAdd), requiredCountAdjustments);
    }
    incrementSequenceAdjustment(createAdjustment(tracedEventToAdd, newNextEvent), requiredCountAdjustments);
  }

  private void removeExistingEventFromTraceAndRecordAdjustments(final List<TracedEventDto> eventTrace,
                                                                final TracedEventDto tracedEventToRemove,
                                                                final Map<String, EventSequenceCountDto> requiredCountAdjustments) {
    int indexOfCurrentEvent = eventTrace.indexOf(tracedEventToRemove);
    TracedEventDto currentPreviousEvent = (indexOfCurrentEvent == 0) ? null : eventTrace.get(indexOfCurrentEvent - 1);
    TracedEventDto currentNextEvent = (indexOfCurrentEvent == eventTrace.size() - 1) ? null : eventTrace.get(
      indexOfCurrentEvent + 1);
    if (currentPreviousEvent != null) {
      decrementSequenceAdjustment(
        createAdjustment(currentPreviousEvent, tracedEventToRemove),
        requiredCountAdjustments
      );
      incrementSequenceAdjustment(createAdjustment(currentPreviousEvent, currentNextEvent), requiredCountAdjustments);
    }
    decrementSequenceAdjustment(createAdjustment(tracedEventToRemove, currentNextEvent), requiredCountAdjustments);
    eventTrace.remove(tracedEventToRemove);
  }

  private void decrementSequenceAdjustment(EventSequenceCountDto adjustment,
                                           Map<String, EventSequenceCountDto> adjustments) {
    addOrUpdateAdjustmentInList(adjustment, adjustments, -1L);
  }

  private void incrementSequenceAdjustment(EventSequenceCountDto adjustment,
                                           Map<String, EventSequenceCountDto> adjustments) {
    addOrUpdateAdjustmentInList(adjustment, adjustments, 1L);
  }

  private void addOrUpdateAdjustmentInList(EventSequenceCountDto sequenceDto,
                                           Map<String, EventSequenceCountDto> adjustments,
                                           Long adjustment) {
    Optional<EventSequenceCountDto> existingAdjustment = Optional.ofNullable(adjustments.get(sequenceDto.getId()));

    if (existingAdjustment.isPresent()) {
      existingAdjustment.get().setCount(existingAdjustment.get().getCount() + adjustment);
    } else {
      sequenceDto.setCount(adjustment);
      adjustments.put(sequenceDto.getId(), sequenceDto);
    }
  }

  private EventSequenceCountDto createAdjustment(TracedEventDto sourceEvent, TracedEventDto targetEvent) {
    EventSequenceCountDto eventSequenceCountDto = EventSequenceCountDto.builder()
      .sourceEvent(Optional.ofNullable(sourceEvent)
                     .map(source -> EventTypeDto.builder()
                       .eventName(sourceEvent.getEventName())
                       .source(sourceEvent.getSource())
                       .group(sourceEvent.getGroup())
                       .build())
                     .orElse(null))
      .targetEvent(Optional.ofNullable(targetEvent)
                     .map(target -> EventTypeDto.builder()
                       .eventName(targetEvent.getEventName())
                       .source(targetEvent.getSource())
                       .group(targetEvent.getGroup())
                       .build())
                     .orElse(null))
      .build();
    eventSequenceCountDto.generateIdForEventSequenceCountDto();
    return eventSequenceCountDto;
  }
}
