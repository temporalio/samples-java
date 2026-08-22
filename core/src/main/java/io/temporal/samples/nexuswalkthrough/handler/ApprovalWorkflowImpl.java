package io.temporal.samples.nexuswalkthrough.handler;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionOutput;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * WALKTHROUGH STEP 4 - Write the approval Workflow (and STEP 7 - the message handlers).
 *
 * <p>The Workflow runs a placeholder Activity that would evaluate whether the request can be
 * auto-decided, runs a placeholder Activity that tells a human the request is waiting, blocks until
 * a decision arrives, and returns it.
 *
 * <p>The blocking step is the reason this is a Workflow rather than an Activity. It may wait weeks,
 * across Worker restarts and deployments, and the wait costs nothing while it is idle.
 */
// @@@SNIPSTART samples-java-nexus-walkthrough-approval-workflow-impl
public class ApprovalWorkflowImpl implements ApprovalWorkflow {

  private static final Logger logger = Workflow.getLogger(ApprovalWorkflowImpl.class);

  private final ApprovalActivities activities =
      Workflow.newActivityStub(
          ApprovalActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  // Durable intermediate state. This is the other reason the approval is a Workflow: an Activity
  // could not hold any of it.
  private SubmitDecisionInput.Decision decision;
  private int remindersSent;
  private final List<String> notes = new ArrayList<>();

  @Override
  public RequestApprovalOutput requestApproval(RequestApprovalInput input) {
    // Placeholder. Real logic would apply policy, check limits, or call a risk service.
    activities.evaluateAutoDecision(input.getItemId(), input.getAmount());

    // Placeholder. Real logic would page an approver, open a ticket, or send email.
    activities.notifyApproverOfPendingRequest(input.getItemId(), input.getRequester());

    // Block until submitDecision supplies a decision. This wait is durable and unbounded - the
    // Worker can restart and redeploy while it is pending.
    Workflow.await(() -> decision != null);

    logger.info(
        "Approval for {} decided {} after {} reminder(s) and {} note(s)",
        input.getItemId(),
        decision.getValue(),
        remindersSent,
        notes.size());

    // This return value becomes the result of the requestApproval Nexus Operation, delivered to
    // every caller whose completion callback is attached to this Execution.
    return new RequestApprovalOutput(Decisions.toRequestApprovalOutput(decision));
  }

  // STEP 7 - Signal handler. Records the nudge and returns nothing.
  @Override
  public void remindApprover() {
    remindersSent++;
    logger.info("Approver reminded, {} reminder(s) so far", remindersSent);
  }

  // STEP 8 - Signal handler reached through Signal-with-Start. When the note arrives before anyone
  // has called requestApproval, the Signal-with-Start creates this Workflow and this handler runs
  // on the fresh Execution.
  @Override
  public void attachContext(String note) {
    notes.add(note);
    logger.info("Context attached: {}", note);
  }

  // STEP 7 - The Update's validator. Runs before the handler and can reject the request without
  // changing anything or writing to Event History. Throwing here rejects the Update; the Workflow
  // is untouched and the caller's Operation fails.
  @Override
  public void validateSubmitDecision(SubmitDecisionInput.Decision decision) {
    if (this.decision != null) {
      throw new IllegalStateException(
          "approval already decided " + this.decision.getValue() + ", cannot decide again");
    }
  }

  // STEP 7 - Update handler. Records the decision, which satisfies the condition the Workflow
  // method is blocked on, and returns confirmation to the caller. The validator above guarantees
  // this runs at most once.
  @Override
  public SubmitDecisionOutput submitDecision(SubmitDecisionInput.Decision decision) {
    this.decision = decision;
    return new SubmitDecisionOutput(Decisions.toSubmitDecisionOutput(decision), remindersSent);
  }
}
// @@@SNIPEND
