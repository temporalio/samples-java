package io.temporal.samples.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.temporal.samples.hello.HelloActivity.GreetingActivities;
import io.temporal.samples.hello.HelloActivity.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloActivity.GreetingWorkflow;
import io.temporal.samples.hello.HelloActivity.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit test for {@link HelloActivity}. Doesn't use an external Temporal service. */
public class HelloActivityJUnit5Test {

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testActivityImpl(
      TestWorkflowEnvironment testEnv, Worker worker, GreetingWorkflow workflow) {
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
  }

  @Test
  public void testMockedActivity(
      TestWorkflowEnvironment testEnv, Worker worker, GreetingWorkflow workflow) {
    // withoutAnnotations() is required to stop Mockito from copying
    // method-level annotations from the GreetingActivities interface
    GreetingActivities activities =
        mock(GreetingActivities.class, withSettings().withoutAnnotations());
    when(activities.composeGreeting("Hello", "World")).thenReturn("Hello World!");
    worker.registerActivitiesImplementations(activities);
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
  }

  @Test
  public void testMockedActivityWithoutSettings(Worker worker) {
    // Mocking activity that has method-level annotations
    // with no withoutAnnotations() setting results in a failure
    GreetingActivities activities = mock(GreetingActivities.class);
    assertThrows(
        IllegalArgumentException.class, () -> worker.registerActivitiesImplementations(activities));
  }
}
