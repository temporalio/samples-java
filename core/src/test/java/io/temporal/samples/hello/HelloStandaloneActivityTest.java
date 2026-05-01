package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloStandaloneActivity.GreetingActivities;
import io.temporal.samples.hello.HelloStandaloneActivity.GreetingActivitiesImpl;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for {@link HelloStandaloneActivity}. Uses an embedded Temporal test server so no
 * external service is required.
 *
 * <p>Standalone Activities do not use a Workflow at runtime, but the embedded test server only
 * supports Activity execution through a Workflow. These tests therefore drive the Activity through
 * a minimal wrapper Workflow so the Activity logic is exercised against the real SDK worker stack.
 */
public class HelloStandaloneActivityTest {

  /**
   * Minimal wrapper Workflow used to invoke {@link GreetingActivities} through the embedded test
   * worker.
   */
  @WorkflowInterface
  public interface TestWorkflow {

    @WorkflowMethod
    String run(String greeting, String name);
  }

  public static class TestWorkflowImpl implements TestWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

    @Override
    public String run(String greeting, String name) {
      return activities.composeGreeting(greeting, name);
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(TestWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testActivityImpl() {
    testWorkflowRule.getWorker().registerActivitiesImplementations(new GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    TestWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TestWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    assertEquals("Hello, World!", workflow.run("Hello", "World"));

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    // withoutAnnotations() prevents Mockito from copying @ActivityMethod from the interface onto
    // the mock, which would cause worker registration to fail.
    GreetingActivities activities =
        mock(GreetingActivities.class, withSettings().withoutAnnotations());
    when(activities.composeGreeting("Hello", "World")).thenReturn("Hello, World!");
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    TestWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TestWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    assertEquals("Hello, World!", workflow.run("Hello", "World"));
    verify(activities).composeGreeting("Hello", "World");

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
