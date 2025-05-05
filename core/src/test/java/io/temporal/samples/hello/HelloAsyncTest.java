

package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloAsync.GreetingActivities;
import io.temporal.samples.hello.HelloAsync.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloAsync.GreetingWorkflow;
import io.temporal.samples.hello.HelloAsync.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloAsync}. Doesn't use an external Temporal service. */
public class HelloAsyncTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testActivityImpl() {
    testWorkflowRule.getWorker().registerActivitiesImplementations(new GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!\nBye World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting("Hello", "World")).thenReturn("Hello World!");
    when(activities.composeGreeting("Bye", "World")).thenReturn("Bye World!");
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!\nBye World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
