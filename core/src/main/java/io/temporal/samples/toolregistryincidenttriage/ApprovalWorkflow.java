package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Companion HITL workflow.
 *
 * <p>The triage agent's request_human_approval tool calls signalWithStart against a deterministic
 * ID per alert group. This workflow stores the latest request, exposes it as a query, and returns
 * the operator's decision.
 */
@WorkflowInterface
public interface ApprovalWorkflow {
  @WorkflowMethod
  Types.ApprovalResponse run(String key);

  @SignalMethod(name = "approval-request")
  void approvalRequest(Types.ApprovalRequest req);

  @SignalMethod(name = "approval-decision")
  void approvalDecision(Types.ApprovalResponse res);

  @QueryMethod(name = "pending-approval")
  Types.ApprovalRequest pendingApproval();
}
