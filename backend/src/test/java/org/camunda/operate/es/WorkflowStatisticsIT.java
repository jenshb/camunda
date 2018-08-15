/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.operate.es;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.camunda.operate.entities.ActivityInstanceEntity;
import org.camunda.operate.entities.ActivityState;
import org.camunda.operate.entities.ActivityType;
import org.camunda.operate.entities.IncidentEntity;
import org.camunda.operate.entities.IncidentState;
import org.camunda.operate.entities.WorkflowInstanceEntity;
import org.camunda.operate.entities.WorkflowInstanceState;
import org.camunda.operate.rest.dto.ActivityStatisticsDto;
import org.camunda.operate.util.DateUtil;
import org.camunda.operate.util.ElasticsearchTestRule;
import org.camunda.operate.util.MockMvcTestRule;
import org.camunda.operate.util.OperateIntegrationTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests Elasticsearch query for workflow statistics.
 */
public class WorkflowStatisticsIT extends OperateIntegrationTest {

  private static final String QUERY_WORKFLOW_STATISTICS_URL = "/api/workflows/%s/statistics";

  private Random random = new Random();

  @Rule
  public ElasticsearchTestRule elasticsearchTestRule = new ElasticsearchTestRule();

  @Rule
  public MockMvcTestRule mockMvcTestRule = new MockMvcTestRule();

  private MockMvc mockMvc;

  @Before
  public void starting() {
    this.mockMvc = mockMvcTestRule.getMockMvc();
  }

  @Test
  public void testOneWorkflowStatistics() throws Exception {
    String workflowId = "demoProcess";

    createData(workflowId);

    MockHttpServletRequestBuilder request = get(getQueryURL(workflowId));

    MvcResult mvcResult = mockMvc.perform(request)
      .andExpect(status().isOk())
      .andExpect(content().contentType(mockMvcTestRule.getContentType()))
      .andReturn();

    final List<ActivityStatisticsDto> activityStatisticsDtos = mockMvcTestRule.listFromResponse(mvcResult, ActivityStatisticsDto.class);

    assertThat(activityStatisticsDtos).hasSize(5);
    assertThat(activityStatisticsDtos).filteredOn(ai -> ai.getActivityId().equals("taskA")).allMatch(ai->
      ai.getActiveCount().equals(2L) && ai.getCanceledCount().equals(0L) && ai.getFinishedCount().equals(0L) && ai.getIncidentsCount().equals(0L)
    );
    assertThat(activityStatisticsDtos).filteredOn(ai -> ai.getActivityId().equals("taskC")).allMatch(ai->
      ai.getActiveCount().equals(0L) && ai.getCanceledCount().equals(2L) && ai.getFinishedCount().equals(0L) && ai.getIncidentsCount().equals(2L)
    );
    assertThat(activityStatisticsDtos).filteredOn(ai -> ai.getActivityId().equals("taskD")).allMatch(ai->
      ai.getActiveCount().equals(0L) && ai.getCanceledCount().equals(1L) && ai.getFinishedCount().equals(0L) && ai.getIncidentsCount().equals(0L)
    );
    assertThat(activityStatisticsDtos).filteredOn(ai -> ai.getActivityId().equals("taskE")).allMatch(ai->
      ai.getActiveCount().equals(1L) && ai.getCanceledCount().equals(0L) && ai.getFinishedCount().equals(0L) && ai.getIncidentsCount().equals(1L)
    );
    assertThat(activityStatisticsDtos).filteredOn(ai -> ai.getActivityId().equals("end")).allMatch(ai->
      ai.getActiveCount().equals(0L) && ai.getCanceledCount().equals(0L) && ai.getFinishedCount().equals(2L) && ai.getIncidentsCount().equals(0L)
    );
  }


  private String getQueryURL(String workflowId) {
    return String.format(QUERY_WORKFLOW_STATISTICS_URL, workflowId);
  }

  /**
   * start
   * taskA  - 2 active
   * taskB
   * taskC  -           - 2 canceled  - 2 with incident
   * taskD  -           - 1 canceled
   * taskE  - 1 active  -             - 1 with incident
   * end    -           -             -                   - 2 finished
   */
  private void createData(String workflowId) {

    List<WorkflowInstanceEntity> instances = new ArrayList<>();

    WorkflowInstanceEntity inst = createWorkflowInstance(WorkflowInstanceState.ACTIVE, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.ACTIVE, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.ACTIVE, "taskA", null));    //duplicated on purpose, to be sure, that we sount workflow instances, but not activity inctanses
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.ACTIVE, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.ACTIVE, "taskA", null));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.CANCELED, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.TERMINATED, "taskC", null));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.CANCELED, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.TERMINATED, "taskC", null));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.ACTIVE, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    ActivityInstanceEntity task = createActivityInstance(ActivityState.INCIDENT, "taskC", null);
    inst.getActivities().add(task);
    inst.getIncidents().add(createIncident(IncidentState.ACTIVE, task.getActivityId(), task.getId()));
    inst.getIncidents().add(createIncident(IncidentState.RESOLVED, task.getActivityId(), task.getId()));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.ACTIVE, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    task = createActivityInstance(ActivityState.INCIDENT, "taskC", null);
    inst.getActivities().add(task);
    inst.getIncidents().add(createIncident(IncidentState.ACTIVE, task.getActivityId(), task.getId()));
    inst.getIncidents().add(createIncident(IncidentState.RESOLVED, task.getActivityId(), task.getId()));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.CANCELED, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskC", null));
    inst.getActivities().add(createActivityInstance(ActivityState.TERMINATED, "taskD", null));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.ACTIVE, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskC", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskD", null));
    inst.getActivities().add(createActivityInstance(ActivityState.ACTIVE, "taskE", null));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.ACTIVE, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskC", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskD", null));
    task = createActivityInstance(ActivityState.INCIDENT, "taskE", null);
    inst.getActivities().add(task);
    inst.getIncidents().add(createIncident(IncidentState.ACTIVE, task.getActivityId(), task.getId()));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.COMPLETED, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskC", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskD", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskE", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "end", ActivityType.END_EVENT));
    instances.add(inst);

    inst = createWorkflowInstance(WorkflowInstanceState.COMPLETED, workflowId);
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "start", ActivityType.START_EVENT));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskA", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskB", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskC", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskD", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "taskE", null));
    inst.getActivities().add(createActivityInstance(ActivityState.COMPLETED, "end", ActivityType.END_EVENT));
    instances.add(inst);

    elasticsearchTestRule.persist(instances.toArray(new WorkflowInstanceEntity[instances.size()]));

  }

  private WorkflowInstanceEntity createWorkflowInstance(WorkflowInstanceState state, String workflowId) {
    WorkflowInstanceEntity workflowInstance = new WorkflowInstanceEntity();
    workflowInstance.setId(UUID.randomUUID().toString());
    workflowInstance.setWorkflowId(workflowId);
    workflowInstance.setBusinessKey("testProcess" + random.nextInt(10));
    workflowInstance.setStartDate(DateUtil.getRandomStartDate());
    if (state.equals(WorkflowInstanceState.COMPLETED) || state.equals(WorkflowInstanceState.CANCELED)) {
      final OffsetDateTime endDate = DateUtil.getRandomEndDate();
      workflowInstance.setEndDate(endDate);
    }
    workflowInstance.setState(state);
    return workflowInstance;
  }

  private IncidentEntity createIncident(IncidentState state, String activityId, String activityInstanceId) {
    IncidentEntity incidentEntity = new IncidentEntity();
    incidentEntity.setId(UUID.randomUUID().toString());
    incidentEntity.setActivityId(activityId);
    incidentEntity.setActivityInstanceId(activityInstanceId);
    incidentEntity.setErrorType("TASK_NO_RETRIES");
    incidentEntity.setErrorMessage("No more retries left.");
    incidentEntity.setState(state);
    return incidentEntity;
  }

  private ActivityInstanceEntity createActivityInstance(ActivityState state, String activityId, ActivityType activityType) {
    ActivityInstanceEntity activityInstanceEntity = new ActivityInstanceEntity();
    activityInstanceEntity.setId(UUID.randomUUID().toString());
    activityInstanceEntity.setActivityId(activityId);
    activityInstanceEntity.setType(activityType);
    activityInstanceEntity.setStartDate(DateUtil.getRandomStartDate());
    activityInstanceEntity.setState(state);
    if (state.equals(ActivityState.COMPLETED) || state.equals(ActivityState.TERMINATED)) {
      activityInstanceEntity.setEndDate(DateUtil.getRandomEndDate());
    }
    return activityInstanceEntity;
  }

}
