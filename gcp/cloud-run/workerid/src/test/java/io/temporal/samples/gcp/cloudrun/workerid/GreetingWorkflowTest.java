package io.temporal.samples.gcp.cloudrun.workerid;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for the sample Workflow and Activity. */
public class GreetingWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setActivityImplementations(new GreetingActivitiesImpl())
          .build();

  @Test
  public void returnsGreeting() {
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    assertEquals("Hello, Cloud Run!", workflow.getGreeting("Cloud Run"));
  }
}
