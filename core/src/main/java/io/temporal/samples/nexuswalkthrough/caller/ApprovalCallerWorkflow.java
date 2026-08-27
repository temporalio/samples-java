package io.temporal.samples.nexuswalkthrough.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * WALKTHROUGH STEP 6 - The caller Workflow.
 *
 * <p>A caller Workflow is the usual pattern, because a Workflow gives the call durability and lets
 * you orchestrate around it. If you only need to run one Operation and have nothing to orchestrate,
 * a Client can start an Operation directly with no caller Workflow at all - that is a Standalone
 * Nexus Operation, and it uses the same contract, handler, and Endpoint.
 */
@WorkflowInterface
public interface ApprovalCallerWorkflow {

  /**
   * Runs the whole approval flow end to end, so one Workflow Execution shows every capability the
   * walkthrough introduces. A real caller would rarely do all of this in one place - in particular,
   * the decision would come from a human through a separate call rather than from the caller
   * itself.
   */
  @WorkflowMethod
  String runApprovalFlow(String itemId, String requester, double amount, String note);
}
