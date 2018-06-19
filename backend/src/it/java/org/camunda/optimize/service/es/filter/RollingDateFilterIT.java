package org.camunda.optimize.service.es.filter;

import org.camunda.optimize.dto.optimize.query.report.result.raw.RawDataReportResultDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.junit.Test;

import java.time.OffsetDateTime;



public class RollingDateFilterIT extends AbstractRollingDateFilterIT {

  @Test
  public void testRollingLogic() throws Exception {
    // given
    embeddedOptimizeRule.reloadConfiguration();
    ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    String processDefinitionId = processInstance.getDefinitionId();
    embeddedOptimizeRule.scheduleAllJobsAndImportEngineEntities();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    RawDataReportResultDto result = createAndEvaluateReport(
        processInstance.getProcessDefinitionKey(),
        processInstance.getProcessDefinitionVersion(),
        "days",
        false
    );

    assertResults(processInstance, result, 1);

    //when
    LocalDateUtil.setCurrentTime(OffsetDateTime.now().plusDays(2));

    //token hast to be refreshed, as the old one expired already after moving the date
    result = createAndEvaluateReport(
        processInstance.getProcessDefinitionKey(),
        processInstance.getProcessDefinitionVersion(),
        "days",
        true
    );

    assertResults(processInstance, result, 0);

    embeddedOptimizeRule.reloadConfiguration();
    embeddedOptimizeRule.getNewAuthenticationToken();
  }

}
