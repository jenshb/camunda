/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.query.report.single.group.GroupByDateUnit;
import org.camunda.optimize.service.es.OptimizeElasticsearchClient;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator;
import org.elasticsearch.search.aggregations.metrics.Stats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.OPTIMIZE_DATE_FORMAT;

@RequiredArgsConstructor
@Component
@Slf4j
public class IntervalAggregationService {

  private static final String STATS_AGGREGATION = "minMaxValueOfData";
  public static final String RANGE_AGGREGATION = "rangeAggregation";

  private final OptimizeElasticsearchClient esClient;
  private final DateTimeFormatter dateTimeFormatter;

  public DateHistogramInterval getDateHistogramInterval(GroupByDateUnit interval) {
    switch (interval) {
      case YEAR:
        return DateHistogramInterval.YEAR;
      case MONTH:
        return DateHistogramInterval.MONTH;
      case WEEK:
        return DateHistogramInterval.WEEK;
      case DAY:
        return DateHistogramInterval.DAY;
      case HOUR:
        return DateHistogramInterval.HOUR;
      case MINUTE:
        return DateHistogramInterval.MINUTE;
      default:
        final String errorMessage =
          String.format("Unknown date interval [%s] for creating a histogram aggregation.", interval);
        log.error(errorMessage);
        throw new OptimizeRuntimeException(errorMessage);
    }
  }

  public Stats getMinMaxStats(QueryBuilder query, String indexName, String field) {
    AggregationBuilder statsAgg = AggregationBuilders
      .stats(STATS_AGGREGATION)
      .field(field)
      .format(OPTIMIZE_DATE_FORMAT);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(statsAgg)
      .size(0);
    SearchRequest searchRequest = new SearchRequest(indexName).source(searchSourceBuilder);

    SearchResponse response;
    try {
      response = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String reason = "Could not automatically determine interval of group by date on field [" + field + "]!";
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }
    return response.getAggregations().get(STATS_AGGREGATION);
  }

  public Optional<AggregationBuilder> createIntervalAggregation(org.apache.commons.lang3.Range<OffsetDateTime> rangeToUse,
                                                                QueryBuilder query,
                                                                String indexName,
                                                                String field) {
    if (rangeToUse != null) {
      OffsetDateTime min = rangeToUse.getMinimum();
      OffsetDateTime max = rangeToUse.getMaximum();
      return Optional.of(createIntervalAggregationFromGivenRange(field, min, max));
    } else {
      return createIntervalAggregation(query, indexName, field);
    }
  }

  private Optional<AggregationBuilder> createIntervalAggregation(QueryBuilder query,
                                                                 String indexName,
                                                                 String field) {
    Stats stats = getMinMaxStats(query, indexName, field);
    if (stats.getCount() > 1) {
      OffsetDateTime min = OffsetDateTime.parse(stats.getMinAsString(), dateTimeFormatter);
      OffsetDateTime max = OffsetDateTime.parse(stats.getMaxAsString(), dateTimeFormatter);
      return Optional.of(createIntervalAggregationFromGivenRange(field, min, max));
    } else {
      return Optional.empty();
    }
  }

  public static long getDateHistogramIntervalInMsFromMinMax(OffsetDateTime min, OffsetDateTime max) {
    long minInMs = min.toInstant().toEpochMilli();
    long maxInMs = max.toInstant().toEpochMilli();
    final long intervalFromMinToMax = (maxInMs - minInMs) / NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION;
    // we need to ensure that the interval is > 1 since we create the range buckets based on this
    // interval and it will cause an endless loop if the interval is 0.
    return Math.max(intervalFromMinToMax, 1);
  }

  public AggregationBuilder createIntervalAggregationFromGivenRange(String field,
                                                                    OffsetDateTime min,
                                                                    OffsetDateTime max) {
    long msAsUnit = getDateHistogramIntervalInMsFromMinMax(min, max);
    RangeAggregationBuilder rangeAgg = AggregationBuilders
      .range(RANGE_AGGREGATION)
      .field(field);
    OffsetDateTime start = min;
    do {
      // this is a do while loop to ensure there's always at least one bucket, even when min and max are equal
      OffsetDateTime nextStart = start.plus(msAsUnit, ChronoUnit.MILLIS);
      boolean isLast = nextStart.isAfter(max) || nextStart.isEqual(max);
      // plus 1 ms because the end of the range is exclusive yet we want to make sure max falls into the last bucket
      OffsetDateTime end = isLast ? nextStart.plus(1, ChronoUnit.MILLIS) : nextStart;

      RangeAggregator.Range range =
        new RangeAggregator.Range(
          dateTimeFormatter.format(start.atZoneSameInstant(ZoneId.systemDefault())), // key that's being used
          dateTimeFormatter.format(start),
          dateTimeFormatter.format(end)
        );
      rangeAgg.addRange(range);
      start = nextStart;
    } while (start.isBefore(max));
    return rangeAgg;
  }

  public Map<String, Range.Bucket> mapIntervalAggregationsToKeyBucketMap(Aggregations aggregations) {
    Range agg = aggregations.get(RANGE_AGGREGATION);

    Map<String, Range.Bucket> result = new LinkedHashMap<>();
    for (Range.Bucket entry : agg.getBuckets()) {
      String formattedDate = entry.getKeyAsString();
      result.put(formattedDate, entry);
    }
    // sort in descending order
    result = result.entrySet().stream()
      .sorted(Collections.reverseOrder(Map.Entry.comparingByKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> {
        throw new IllegalStateException(String.format("Duplicate key %s", u));
      }, LinkedHashMap::new));
    return result;
  }
}
