package io.temporal.samples.dsl;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.dsl.model.Flow;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class DslWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(DslWorkflowImpl.class)
          .setActivityImplementations(new DslActivitiesImpl())
          .build();

  @Test
  public void testDslWorkflow() throws Exception {
    DslWorkflow workflow =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowClient()
            .newWorkflowStub(
                DslWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("dsl-workflow")
                    .setTaskQueue(testWorkflowRule.getWorker().getTaskQueue())
                    .build());

    String result = workflow.run(getFlowFromResource(), "test input");
    assertNotNull(result);
    assertEquals(
        "Activity one done...,Activity two done...,Activity three done...,Activity four done...",
        result);
  }

  private static Flow getFlowFromResource() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(
          DslWorkflowTest.class.getClassLoader().getResource("dsl/sampleflow.json"), Flow.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
