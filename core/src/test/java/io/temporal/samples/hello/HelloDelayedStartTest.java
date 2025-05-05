

package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class HelloDelayedStartTest {
  private final String WORKFLOW_ID = "HelloDelayedStartWorkflow";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloDelayedStart.DelayedStartWorkflowImpl.class)
          .build();

  @Test
  public void testDelayedStart() {
    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(WORKFLOW_ID)
            .setStartDelay(Duration.ofSeconds(2))
            .build();

    HelloDelayedStart.DelayedStartWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(HelloDelayedStart.DelayedStartWorkflow.class, workflowOptions);

    workflow.start();

    // Fetch event history and make sure we got the 2 seconds first workflow task backoff
    WorkflowExecutionHistory history =
        testWorkflowRule.getWorkflowClient().fetchHistory(WORKFLOW_ID);
    com.google.protobuf.Duration backoff =
        history
            .getHistory()
            .getEvents(0)
            .getWorkflowExecutionStartedEventAttributes()
            .getFirstWorkflowTaskBackoff();

    assertEquals(2, backoff.getSeconds());
  }
}
