package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloActivityRetry.GreetingActivities;
import io.temporal.samples.hello.HelloActivityRetry.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloActivityRetry.GreetingWorkflow;
import io.temporal.samples.hello.HelloActivityRetry.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Unit test for {@link HelloActivityRetry}. Doesn't use an external Temporal service. */
public class HelloActivityRetryTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  /** Prints a history of the workflow under test in case of a test failure. */
  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testWorkflowRule.getTestEnvironment() != null) {
            System.err.println(testWorkflowRule.getTestEnvironment().getDiagnostics());
            testWorkflowRule.getTestEnvironment().shutdown();
          }
        }
      };

  @Test
  public void testActivityImpl() {
    testWorkflowRule.getWorker().registerActivitiesImplementations(new GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    // Execute a workflow waiting for it to complete
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting("Hello", "World"))
        .thenThrow(
            new IllegalStateException("not yet1"),
            new IllegalStateException("not yet2"),
            new IllegalStateException("not yet3"))
        .thenReturn("Hello World!");
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
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
    assertEquals("Hello World!", greeting);

    verify(activities, times(4)).composeGreeting(anyString(), anyString());

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
