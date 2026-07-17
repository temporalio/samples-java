package io.temporal.samples.gcp;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class GreetingWorkflowTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void completesGreeting() {
    testWorkflowRule.getWorker().registerActivitiesImplementations(new GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    assertEquals("Hello Google Cloud!", workflow.getGreeting("Google Cloud"));
  }
}
