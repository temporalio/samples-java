

package io.temporal.samples.terminateworkflow;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.TerminatedFailure;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class TerminateWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setWorkflowTypes(MyWorkflowImpl.class).build();

  @Test
  public void testTerminateWorkflow() {
    WorkflowStub wfs =
        testWorkflowRule
            .getWorkflowClient()
            .newUntypedWorkflowStub(
                "MyWorkflow",
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    wfs.start(testWorkflowRule.getTaskQueue());
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    wfs.terminate("Test Reasons");
    try {
      wfs.getResult(String.class);
      fail("unreachable");
    } catch (WorkflowFailedException ignored) {
      assertTrue(ignored.getCause() instanceof TerminatedFailure);
      assertEquals("Test Reasons", ((TerminatedFailure) ignored.getCause()).getOriginalMessage());
    }
  }
}
