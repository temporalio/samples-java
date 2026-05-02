package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.workflow.Workflow;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {
  private Types.ApprovalRequest request;
  private Types.ApprovalResponse response;

  @Override
  public Types.ApprovalResponse run(String key) {
    // Wait for the agent's request, then the operator's decision.
    // LLM retry: re-attached requests overwrite prior state — operator only
    // ever sees the latest version, since the agent may refine its ask.
    Workflow.await(() -> request != null);
    Workflow.await(() -> response != null);
    return response;
  }

  @Override
  public void approvalRequest(Types.ApprovalRequest req) {
    this.request = req;
  }

  @Override
  public void approvalDecision(Types.ApprovalResponse res) {
    this.response = res;
  }

  @Override
  public Types.ApprovalRequest pendingApproval() {
    return request;
  }
}
