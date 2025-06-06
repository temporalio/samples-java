package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloPolymorphicActivity.ByeActivityImpl;
import io.temporal.samples.hello.HelloPolymorphicActivity.GreetingWorkflow;
import io.temporal.samples.hello.HelloPolymorphicActivity.GreetingWorkflowImpl;
import io.temporal.samples.hello.HelloPolymorphicActivity.HelloActivityImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloActivity}. Doesn't use an external Temporal service. */
public class HelloPolymorphicActivityTest {

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
        .registerActivitiesImplementations(new HelloActivityImpl(), new ByeActivityImpl());
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
    assertEquals("Hello World!\nBye World!\n", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    HelloPolymorphicActivity.HelloActivity hello =
        mock(HelloPolymorphicActivity.HelloActivity.class);
    when(hello.composeGreeting("World")).thenReturn("Hello World!");
    HelloPolymorphicActivity.ByeActivity bye = mock(HelloPolymorphicActivity.ByeActivity.class);
    when(bye.composeGreeting("World")).thenReturn("Bye World!");
    testWorkflowRule.getWorker().registerActivitiesImplementations(hello, bye);
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
    assertEquals("Hello World!\nBye World!\n", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
