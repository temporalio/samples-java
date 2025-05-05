package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloAsyncLambda.GreetingActivities;
import io.temporal.samples.hello.HelloAsyncLambda.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloAsyncLambda.GreetingWorkflow;
import io.temporal.samples.hello.HelloAsyncLambda.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/** Unit test for {@link HelloAsyncLambda}. Doesn't use an external Temporal service. */
public class HelloAsyncLambdaTest {

  @Rule public Timeout globalTimeout = Timeout.seconds(3);

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

    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!\nHello World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.getGreeting()).thenReturn("Hello");
    when(activities.composeGreeting("Hello", "World")).thenReturn("Hello World!");
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!\nHello World!", greeting);

    verify(activities, times(2)).composeGreeting(anyString(), anyString());
    verify(activities, times(2)).getGreeting();

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
