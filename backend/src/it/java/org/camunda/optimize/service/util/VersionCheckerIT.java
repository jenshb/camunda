package org.camunda.optimize.service.util;

import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class VersionCheckerIT {
  public EngineIntegrationRule engineRule = new EngineIntegrationRule();
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule2 = new EmbeddedOptimizeRule("classpath:versionCheckContext.xml");

  @Rule
  public RuleChain chain = RuleChain
    .outerRule(elasticSearchRule)
    .around(engineRule)
    .around(embeddedOptimizeRule2);

  @Test
  public void engineVersionCantBeDetermined () throws Exception {
    embeddedOptimizeRule2.stopOptimize();

    try {
      embeddedOptimizeRule2.startOptimize();
    } catch (Exception e) {
      //expected
      assertThat(e.getCause().getMessage().contains("Engine version is not supported"), is(true));
      return;
    }

    fail("Exception expected");
  }
  @After
  public void setContextBack() throws Exception {
    embeddedOptimizeRule2.stopOptimize();
    EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule("classpath:embeddedOptimizeContext.xml");
    embeddedOptimizeRule.startOptimize();
  }
}
