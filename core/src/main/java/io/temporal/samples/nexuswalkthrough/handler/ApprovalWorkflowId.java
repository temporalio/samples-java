package io.temporal.samples.nexuswalkthrough.handler;

/**
 * WALKTHROUGH STEP 3 - Give the approval Workflow a stable Id.
 *
 * <p>The approval needs a Workflow Id derived from the purchase, not a random one, so that later
 * messages can find it. Deriving it from the item id means a caller that knows the item id can
 * reach the right Execution without the handler handing out Workflow Ids.
 *
 * <p>This also makes the start idempotent: a retried Nexus start request targets the same Workflow
 * Id rather than starting a second approval for one purchase.
 *
 * <p>It matters again in step 8, where attachApprovalContext may start the approval before
 * requestApproval is ever called. Both Operations derive the same Workflow Id from the same item
 * id, which is what lets them agree on which Execution they mean. That is why this lives in one
 * place instead of being spelled out at each call site.
 */
public final class ApprovalWorkflowId {

  private ApprovalWorkflowId() {}

  // @@@SNIPSTART samples-java-nexus-walkthrough-workflow-id
  public static String forItem(String itemId) {
    return "approval-" + itemId;
  }
  // @@@SNIPEND
}
