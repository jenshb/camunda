package org.camunda.optimize.service.engine.importing.fetcher.count;

import org.camunda.optimize.dto.engine.CountDto;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.service.engine.importing.fetcher.AbstractEngineAwareFetcher;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;

import static org.camunda.optimize.service.engine.importing.fetcher.instance.EngineEntityFetcher.UTF8;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.INCLUDE_ONLY_UNFINISHED_INSTANCES;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.TRUE;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UnfinishedProcessInstanceCountFetcher extends AbstractEngineAwareFetcher {

  public UnfinishedProcessInstanceCountFetcher(EngineContext engineContext) {
    super(engineContext);
  }

  public Long fetchUnfinishedHistoricProcessInstanceCount() {
    long totalCount = 0;

    CountDto newCount = getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(configurationService.getHistoricProcessInstanceCountEndpoint())
        .queryParam(INCLUDE_ONLY_UNFINISHED_INSTANCES, TRUE)
        .request()
        .acceptEncoding(UTF8)
        .get(CountDto.class);
    totalCount += newCount.getCount();

    return totalCount;
  }
}
