package io.temporal.samples.hello;

import static org.junit.Assert.assertTrue;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloCancellationScope.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloCancellationScope.GreetingWorkflow;
import io.temporal.samples.hello.HelloCancellationScope.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloCancellationScope}. Doesn't use an external Temporal service. */
public class HelloCancellationScopeTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setActivityImplementations(new GreetingActivitiesImpl())
          .build();

  @Test(timeout = 240_000)
  public void testActivityImpl() {
    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertTrue(greeting.endsWith(" World!"));
  }
}
