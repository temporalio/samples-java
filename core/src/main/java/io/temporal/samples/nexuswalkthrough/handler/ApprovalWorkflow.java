package io.temporal.samples.nexuswalkthrough.handler;

import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionOutput;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * WALKTHROUGH STEP 4 - The approval Workflow, plus STEP 7 - the message handlers.
 *
 * <p>This is an ordinary Temporal Workflow. Nothing in it is Nexus-specific, and it could be
 * started directly by a Client instead of through a Nexus Operation.
 *
 * <p>What makes it interactive is the message handlers below and a Workflow Id you can predict (see
 * {@link ApprovalWorkflowId}), not a special kind of Workflow.
 *
 * <p>The types it takes and returns are the generated ones from step 2. Nothing here is
 * hand-written, which is what keeps the Workflow and the contract from drifting apart.
 */
@WorkflowInterface
// @@@SNIPSTART samples-java-nexus-walkthrough-approval-workflow
public interface ApprovalWorkflow {

  /**
   * The Update handler's name on the wire. The Update-backed Operation has to name the Update
   * explicitly when it starts one, so the name is declared once here and reused there rather than
   * being spelled as a literal in two places.
   */
  String SUBMIT_DECISION_UPDATE = "submitDecision";

  /**
   * STEP 4 - The Workflow method. Its return value is the result of the requestApproval Operation:
   * the Operation completes when this Workflow returns, and the caller receives this value through
   * the Nexus completion callback.
   *
   * <p>Because the Workflow's return value is delivered straight to the caller as the Operation
   * result, it has to be the Operation's declared output type.
   */
  @WorkflowMethod
  RequestApprovalOutput runApproval(RequestApprovalInput input);

  /**
   * STEP 7 - A Signal. Fire-and-forget: the caller gets no result back, which is why a Signal is
   * the right message type for a nudge, and why the contract declares no output for it.
   */
  @SignalMethod
  void remindApprover();

  /**
   * STEP 8 - A Signal that also carries supporting information. Reached through Signal-with-Start,
   * so it may be the message that creates this Workflow.
   */
  @SignalMethod
  void attachContext(String note);

  /**
   * STEP 7 - An Update. The caller needs a result back - confirmation that the decision was
   * recorded - which is what makes this an Update rather than a Signal.
   */
  @UpdateMethod(name = ApprovalWorkflow.SUBMIT_DECISION_UPDATE)
  SubmitDecisionOutput submitDecision(SubmitDecisionInput.Decision decision);

  /**
   * STEP 7 - The Update's validator. An Update can reject a request before it changes anything,
   * which a Signal cannot: a Signal has already been accepted by the time the handler runs.
   *
   * <p>Here it rejects a second decision for an approval that has already been decided. Without it
   * the later decision would silently overwrite the earlier one. A rejected Update does not appear
   * in Event History and does not run the handler.
   */
  @UpdateValidatorMethod(updateName = ApprovalWorkflow.SUBMIT_DECISION_UPDATE)
  void validateSubmitDecision(SubmitDecisionInput.Decision decision);
}
// @@@SNIPEND
