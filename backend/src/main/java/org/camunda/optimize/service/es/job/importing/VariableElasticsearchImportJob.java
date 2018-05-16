package org.camunda.optimize.service.es.job.importing;

import org.camunda.optimize.dto.optimize.query.variable.VariableDto;
import org.camunda.optimize.service.es.job.ElasticsearchImportJob;
import org.camunda.optimize.service.es.writer.variable.FinalVariableInstanceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class VariableElasticsearchImportJob extends ElasticsearchImportJob<VariableDto> {

  private FinalVariableInstanceWriter variableWriter;
  private Logger logger = LoggerFactory.getLogger(getClass());

  private List<String> processInstanceIdsVariablesHaveBeenImportedFor = new ArrayList<>();

  public VariableElasticsearchImportJob(FinalVariableInstanceWriter variableWriter) {
    this.variableWriter = variableWriter;
  }

  @Override
  protected void executeImport() {
    try {
      variableWriter.importVariables(newOptimizeEntities);
      variableWriter.flagProcessInstancesAllVariablesHaveBeenImportedFor(processInstanceIdsVariablesHaveBeenImportedFor);
    } catch (Exception e) {
      logger.error("error while writing variables to elasticsearch", e);
    }
  }

  public void setProcessInstanceIdsVariablesHaveBeenImportedFor(List<String> processInstanceIdsVariablesHaveBeenImportedFor) {
    this.processInstanceIdsVariablesHaveBeenImportedFor = processInstanceIdsVariablesHaveBeenImportedFor;
  }
}
