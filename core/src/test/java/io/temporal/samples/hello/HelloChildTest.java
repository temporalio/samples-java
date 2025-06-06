package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloChild.GreetingChild;
import io.temporal.samples.hello.HelloChild.GreetingChildImpl;
import io.temporal.samples.hello.HelloChild.GreetingWorkflow;
import io.temporal.samples.hello.HelloChild.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloChild}. Doesn't use an external Temporal service. */
public class HelloChildTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setDoNotStart(true).build();

  @Test
  public void testChild() {
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
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
  public void testMockedChild() {
    testWorkflowRule.getWorker().registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    // As new mock is created on each workflow task the only last one is useful to verify calls.
    AtomicReference<GreetingChild> lastChildMock = new AtomicReference<>();
    // Factory is called to create a new workflow object on each workflow task.
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationFactory(
            GreetingChild.class,
            () -> {
              GreetingChild child = mock(GreetingChild.class);
              when(child.composeGreeting("Hello", "World")).thenReturn("Hello World!");
              lastChildMock.set(child);
              return child;
            });

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
    GreetingChild mock = lastChildMock.get();
    verify(mock).composeGreeting(eq("Hello"), eq("World"));

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
