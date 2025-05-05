package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloEagerWorkflowStart.GreetingActivities;
import io.temporal.samples.hello.HelloEagerWorkflowStart.GreetingLocalActivitiesImpl;
import io.temporal.samples.hello.HelloEagerWorkflowStart.GreetingWorkflow;
import io.temporal.samples.hello.HelloEagerWorkflowStart.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloEagerWorkflowStart}. Doesn't use an external Temporal service. */
public class HelloEagerWorkflowStartTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testActivityImpl() {
    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new GreetingLocalActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    // withoutAnnotations() is required to stop Mockito from copying
    // method-level annotations from the GreetingActivities interface
    GreetingActivities activities =
        mock(GreetingActivities.class, withSettings().withoutAnnotations());
    when(activities.composeGreeting("Hello", "World")).thenReturn("Hello World!");
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
