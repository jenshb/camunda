/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.command.process.processinstance.duration.groupby.date.distributedby.none;

import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.service.db.es.report.command.ProcessCmd;
import io.camunda.optimize.service.db.es.report.command.exec.ProcessReportCmdExecutionPlan;
import io.camunda.optimize.service.db.es.report.command.exec.builder.ReportCmdExecutionPlanBuilder;
import io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.ProcessDistributedByNone;
import io.camunda.optimize.service.db.es.report.command.modules.group_by.process.date.ProcessGroupByProcessInstanceStartDate;
import io.camunda.optimize.service.db.es.report.command.modules.view.process.duration.ProcessViewInstanceDurationOnProcessPart;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceDurationOnProcessPartGroupByStartDateCmd
    extends ProcessCmd<List<MapResultEntryDto>> {

  public ProcessInstanceDurationOnProcessPartGroupByStartDateCmd(
      final ReportCmdExecutionPlanBuilder builder) {
    super(builder);
  }

  @Override
  protected ProcessReportCmdExecutionPlan<List<MapResultEntryDto>> buildExecutionPlan(
      final ReportCmdExecutionPlanBuilder builder) {
    return builder
        .createExecutionPlan()
        .processCommand()
        .view(ProcessViewInstanceDurationOnProcessPart.class)
        .groupBy(ProcessGroupByProcessInstanceStartDate.class)
        .distributedBy(ProcessDistributedByNone.class)
        .resultAsMap()
        .build();
  }
}