package io.temporal.samples.nexuswalkthrough;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionOutput;
import io.temporal.samples.nexuswalkthrough.handler.ApprovalActivitiesImpl;
import io.temporal.samples.nexuswalkthrough.handler.ApprovalWorkflow;
import io.temporal.samples.nexuswalkthrough.handler.ApprovalWorkflowId;
import io.temporal.samples.nexuswalkthrough.handler.ApprovalWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the approval Workflow that backs the requestApproval Nexus Operation.
 *
 * <p>The Workflow is an ordinary Temporal Workflow with nothing Nexus-specific in it, which is what
 * makes it testable on its own. These tests cover the behavior the walkthrough relies on: the
 * Workflow blocks until a decision arrives, counts reminders, and refuses a second decision.
 */
public class ApprovalWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(ApprovalWorkflowImpl.class)
          .setActivityImplementations(new ApprovalActivitiesImpl())
          .build();

  private ApprovalWorkflow newApproval(String itemId) {
    return testWorkflowRule
        .getWorkflowClient()
        .newWorkflowStub(
            ApprovalWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(ApprovalWorkflowId.forItem(itemId))
                .build());
  }

  /** The decision submitted through the Update becomes the Workflow's return value. */
  @Test
  public void decisionSubmittedThroughUpdateBecomesTheResult() {
    ApprovalWorkflow approval = newApproval("standing-desk");
    WorkflowClient.start(
        approval::requestApproval,
        new RequestApprovalInput("standing-desk", "dana@example.com", 1250.00));

    SubmitDecisionOutput ack =
        approval.submitDecision(SubmitDecisionInput.Decision.DECISION_APPROVED);
    assertEquals("APPROVED", ack.getRecorded().getValue());
    assertEquals(0, ack.getRemindersSent());

    RequestApprovalOutput result =
        WorkflowStub.fromTyped(approval).getResult(RequestApprovalOutput.class);
    assertEquals("APPROVED", result.getDecision().getValue());
  }

  /** Reminders are counted while the approval is pending and reported back with the decision. */
  @Test
  public void remindersAreCountedAndReportedWithTheDecision() {
    ApprovalWorkflow approval = newApproval("monitor-arm");
    WorkflowClient.start(
        approval::requestApproval,
        new RequestApprovalInput("monitor-arm", "dana@example.com", 900.00));

    approval.remindApprover();
    approval.remindApprover();

    SubmitDecisionOutput ack =
        approval.submitDecision(SubmitDecisionInput.Decision.DECISION_DENIED);
    assertEquals("DENIED", ack.getRecorded().getValue());
    assertEquals(2, ack.getRemindersSent());
  }

  /**
   * Context attached before the approval exists is what Signal-with-Start delivers in step 8. Here
   * it is sent directly, to check the handler accepts it alongside the rest of the flow.
   */
  @Test
  public void contextCanBeAttachedWhileTheApprovalIsPending() {
    ApprovalWorkflow approval = newApproval("laptop-dock");
    WorkflowClient.start(
        approval::requestApproval,
        new RequestApprovalInput("laptop-dock", "dana@example.com", 750.00));

    approval.attachContext("Approved in the Q3 ergonomics budget");
    SubmitDecisionOutput ack =
        approval.submitDecision(SubmitDecisionInput.Decision.DECISION_APPROVED);

    assertEquals("APPROVED", ack.getRecorded().getValue());
  }

  /**
   * The Update validator rejects a second decision rather than letting it overwrite the first. A
   * rejected Update never runs the handler and never reaches Event History.
   */
  @Test
  public void aSecondDecisionIsRejected() {
    ApprovalWorkflow approval = newApproval("desk-lamp");
    WorkflowClient.start(
        approval::requestApproval,
        new RequestApprovalInput("desk-lamp", "dana@example.com", 600.00));

    approval.submitDecision(SubmitDecisionInput.Decision.DECISION_APPROVED);

    WorkflowUpdateException failure =
        assertThrows(
            WorkflowUpdateException.class,
            () -> approval.submitDecision(SubmitDecisionInput.Decision.DECISION_DENIED));
    assertTrue(failure.getCause().getMessage().contains("already decided"));
  }
}
