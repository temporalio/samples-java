package io.temporal.samples.hello;

import static org.junit.Assert.assertTrue;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class HelloCancellationScopeWithTimerTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloCancellationScopeWithTimer.CancellationWithTimerWorkflowImpl.class)
          .setActivityImplementations(
              new HelloCancellationScopeWithTimer.UpdateInfoActivitiesImpl())
          .build();

  @Test(timeout = 240_000)
  public void testActivityImpl() {
    // Get a workflow stub using the same task queue the worker uses.
    HelloCancellationScopeWithTimer.CancellationWithTimerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloCancellationScopeWithTimer.CancellationWithTimerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String result = workflow.execute("Test Input");
    assertTrue(result.endsWith("Activity canceled due to timer firing."));
  }
}
