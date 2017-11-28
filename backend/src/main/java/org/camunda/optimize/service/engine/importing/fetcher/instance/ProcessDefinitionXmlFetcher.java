package org.camunda.optimize.service.engine.importing.fetcher.instance;

import org.camunda.optimize.dto.engine.ProcessDefinitionEngineDto;
import org.camunda.optimize.dto.engine.ProcessDefinitionXmlEngineDto;
import org.camunda.optimize.service.engine.importing.index.page.AllEntitiesBasedImportPage;
import org.camunda.optimize.service.engine.importing.index.page.DefinitionBasedImportPage;
import org.camunda.optimize.service.util.BeanHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProcessDefinitionXmlFetcher
  extends RetryBackoffEngineEntityFetcher<ProcessDefinitionXmlEngineDto, DefinitionBasedImportPage> {
  private ProcessDefinitionFetcher processDefinitionFetcher;

  @Autowired
  private BeanHelper beanHelper;


  public ProcessDefinitionXmlFetcher(String engineAlias) {
    super(engineAlias);
  }

  @PostConstruct
  public void init() {
    processDefinitionFetcher = beanHelper.getInstance(ProcessDefinitionFetcher.class, engineAlias);
  }

  @Override
  protected List<ProcessDefinitionXmlEngineDto> fetchEntities(DefinitionBasedImportPage page) {
    return fetchProcessDefinitionXmls(page);
  }

  public List<ProcessDefinitionXmlEngineDto> fetchProcessDefinitionXmls(DefinitionBasedImportPage importIndex) {
    List<ProcessDefinitionEngineDto> entries =
      processDefinitionFetcher.fetchEngineEntities(importIndex);
    return fetchAllXmls(entries);
  }

  private List<ProcessDefinitionXmlEngineDto> fetchAllXmls(List<ProcessDefinitionEngineDto> entries) {
    List<ProcessDefinitionXmlEngineDto> xmls = new ArrayList<>(entries.size());
    long requestStart = System.currentTimeMillis();
    for (ProcessDefinitionEngineDto engineDto : entries) {
      ProcessDefinitionXmlEngineDto xml = getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(configurationService.getProcessDefinitionXmlEndpoint(engineDto.getId()))
        .request(MediaType.APPLICATION_JSON)
        .get(ProcessDefinitionXmlEngineDto.class);
      xmls.add(xml);
    }
    long requestEnd = System.currentTimeMillis();
    logger.debug(
      "Fetched [{}] process definition xmls within [{}] ms",
      entries.size(),
      requestEnd - requestStart
    );


    return xmls;
  }
}
