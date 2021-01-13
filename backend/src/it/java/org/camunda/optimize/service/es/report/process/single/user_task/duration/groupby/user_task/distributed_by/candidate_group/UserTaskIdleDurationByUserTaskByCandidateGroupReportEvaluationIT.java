/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.process.single.user_task.duration.groupby.user_task.distributed_by.candidate_group;

import org.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.CanceledFlowNodesOnlyFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.CompletedOrCanceledFlowNodesOnlyFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.RunningFlowNodesOnlyFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.hyper.ReportHyperMapResultDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.es.report.util.HyperMapAsserter;
import org.camunda.optimize.test.util.TemplatedProcessReportDataBuilder;

import java.util.List;

import static org.camunda.optimize.test.util.ProcessReportDataType.USER_TASK_DURATION_GROUP_BY_USER_TASK_BY_CANDIDATE_GROUP;
import static org.junit.jupiter.api.Assertions.fail;

public class UserTaskIdleDurationByUserTaskByCandidateGroupReportEvaluationIT
  extends AbstractUserTaskDurationByUserTaskByCandidateGroupReportEvaluationIT {

  @Override
  protected UserTaskDurationTime getUserTaskDurationTime() {
    return UserTaskDurationTime.IDLE;
  }

  @Override
  protected void changeDuration(final ProcessInstanceEngineDto processInstanceDto, final Double durationInMs) {
    changeUserTaskIdleDuration(processInstanceDto, durationInMs);
  }

  @Override
  protected void changeDuration(final ProcessInstanceEngineDto processInstanceDto,
                                final String userTaskKey,
                                final Double durationInMs) {
    changeUserTaskIdleDuration(processInstanceDto, userTaskKey, durationInMs);
  }

  @Override
  protected ProcessReportDataDto createReport(final String processDefinitionKey, final List<String> versions) {
    return TemplatedProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersions(versions)
      .setUserTaskDurationTime(UserTaskDurationTime.IDLE)
      .setReportDataType(USER_TASK_DURATION_GROUP_BY_USER_TASK_BY_CANDIDATE_GROUP)
      .build();
  }

  @Override
  protected void assertEvaluateReportWithFlowNodeStatusFilter(final ReportHyperMapResultDto result,
                                                              final List<ProcessFilterDto<?>> processFilter) {
    if (isSingleFilterOfType(processFilter, RunningFlowNodesOnlyFilterDto.class)) {
      // @formatter:off
      HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .isComplete(true)
          .groupByContains(USER_TASK_1)
            .distributedByContains(FIRST_CANDIDATE_GROUP_ID, 200., FIRST_CANDIDATE_GROUP_NAME)
          .groupByContains(USER_TASK_2)
            .distributedByContains(FIRST_CANDIDATE_GROUP_ID, 200., FIRST_CANDIDATE_GROUP_NAME)
        .doAssert(result);
      // @formatter:on
    } else if (isSingleFilterOfType(processFilter, CompletedOrCanceledFlowNodesOnlyFilterDto.class)) {
      // @formatter:off
      HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .isComplete(true)
          .groupByContains(USER_TASK_1)
            .distributedByContains(FIRST_CANDIDATE_GROUP_ID, 100., FIRST_CANDIDATE_GROUP_NAME)
        .doAssert(result);
      // @formatter:on
    } else if (isSingleFilterOfType(processFilter, CanceledFlowNodesOnlyFilterDto.class)) {
      // @formatter:off
      HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .isComplete(true)
          .groupByContains(USER_TASK_1)
            .distributedByContains(FIRST_CANDIDATE_GROUP_ID, 700., FIRST_CANDIDATE_GROUP_NAME)
          .groupByContains(USER_TASK_2)
            .distributedByContains(FIRST_CANDIDATE_GROUP_ID, 700., FIRST_CANDIDATE_GROUP_NAME)
        .doAssert(result);
      // @formatter:on
    } else {
      fail("No assertions for execution state: " + processFilter);
    }
  }
}
