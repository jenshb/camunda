package org.camunda.optimize.service.engine.importing.job;

import org.camunda.optimize.dto.engine.ProcessDefinitionXmlEngineDto;
import org.camunda.optimize.dto.optimize.importing.ProcessDefinitionXmlOptimizeDto;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.service.engine.importing.diff.MissingEntitiesFinder;
import org.camunda.optimize.service.engine.importing.fetcher.instance.EngineEntityFetcher;
import org.camunda.optimize.service.engine.importing.index.page.DefinitionBasedImportPage;
import org.camunda.optimize.service.es.ElasticsearchImportJobExecutor;
import org.camunda.optimize.service.es.job.ElasticsearchImportJob;
import org.camunda.optimize.service.es.job.importing.ProcessDefinitionXmlElasticsearchImportJob;
import org.camunda.optimize.service.es.writer.ProcessDefinitionWriter;

import java.util.List;

public class ProcessDefinitionXmlEngineImportJob extends
    EngineImportJob<ProcessDefinitionXmlEngineDto, ProcessDefinitionXmlOptimizeDto, DefinitionBasedImportPage> {

  private ProcessDefinitionWriter processDefinitionWriter;

  public ProcessDefinitionXmlEngineImportJob(
      ProcessDefinitionWriter processDefinitionWriter,
      DefinitionBasedImportPage importIndex,
      ElasticsearchImportJobExecutor elasticsearchImportJobExecutor,
      MissingEntitiesFinder<ProcessDefinitionXmlEngineDto> missingEntitiesFinder,
      EngineEntityFetcher<ProcessDefinitionXmlEngineDto, DefinitionBasedImportPage> engineEntityFetcher,
      EngineContext engineContext
  ) {
    super(importIndex, elasticsearchImportJobExecutor, missingEntitiesFinder, engineEntityFetcher, engineContext);
    this.processDefinitionWriter = processDefinitionWriter;

  }

  @Override
  protected ElasticsearchImportJob<ProcessDefinitionXmlOptimizeDto>
  createElasticsearchImportJob(List<ProcessDefinitionXmlOptimizeDto> processDefinitions) {
    ProcessDefinitionXmlElasticsearchImportJob procDefImportJob = new ProcessDefinitionXmlElasticsearchImportJob(processDefinitionWriter);
    procDefImportJob.setEntitiesToImport(processDefinitions);
    return procDefImportJob;
  }

  @Override
  protected ProcessDefinitionXmlOptimizeDto mapEngineEntityToOptimizeEntity(ProcessDefinitionXmlEngineDto engineEntity) {
    ProcessDefinitionXmlOptimizeDto optimizeDto = new ProcessDefinitionXmlOptimizeDto();
    optimizeDto.setBpmn20Xml(engineEntity.getBpmn20Xml());
    optimizeDto.setId(engineEntity.getId());
    optimizeDto.setEngine(engineContext.getEngineAlias());
    return optimizeDto;
  }

}
