package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloSignal.GreetingWorkflow;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloSignal}. Doesn't use an external Temporal service. */
public class HelloSignalTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloSignal.GreetingWorkflowImpl.class)
          .build();

  @Test
  public void testSignal() {
    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowIdReusePolicy(
                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
            .build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously to not use another thread to signal.
    WorkflowClient.start(workflow::getGreetings);

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub immediately.
    // But just to demonstrate the unit testing of a long running workflow adding a long sleep here.
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofDays(1));
    // This workflow keeps receiving signals until exit is called
    workflow.waitForName("World");
    workflow.waitForName("Universe");
    workflow.exit();
    // Calling synchronous getGreeting after workflow has started reconnects to the existing
    // workflow and
    // blocks until result is available. Note that this behavior assumes that WorkflowOptions are
    // not configured
    // with WorkflowIdReusePolicy.AllowDuplicate. In that case the call would fail with
    // WorkflowExecutionAlreadyStartedException.
    List<String> greetings = workflow.getGreetings();
    assertEquals(2, greetings.size());
    assertEquals("Hello World!", greetings.get(0));
    assertEquals("Hello Universe!", greetings.get(1));
  }
}
