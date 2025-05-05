

package io.temporal.samples.hello;

import static org.mockito.Mockito.*;

import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit test for {@link HelloChild}. Doesn't use an external Temporal service. */
public class HelloChildJUnit5Test {
  private HelloChild.GreetingChild child = mock(HelloChild.GreetingChildImpl.class);

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(HelloChild.GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testChild(
      TestWorkflowEnvironment testEnv, Worker worker, HelloChild.GreetingWorkflow workflow) {
    worker.registerWorkflowImplementationFactory(
        HelloChild.GreetingChild.class,
        () -> {
          when(child.composeGreeting(anyString(), anyString())).thenReturn("Bye World!");
          return child;
        });
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    Assert.assertEquals("Bye World!", greeting);
    verify(child).composeGreeting(eq("Hello"), eq("World"));

    testEnv.shutdown();
  }
}
