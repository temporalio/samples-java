package io.temporal.samples.nexuswalkthrough.handler;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterOutput;

/**
 * Activities used by this sample.
 *
 * <p>Two of them are placeholders called from inside the approval Workflow (step 4). The third,
 * {@code notifyRequester}, is the one backing a Nexus Operation directly as a Standalone Activity
 * (step 9) - it is never called from a Workflow in this sample.
 *
 * <p>Nothing in any of them is Nexus-specific. The same Activity Function can be invoked from a
 * Workflow and started behind an Operation with no code changes; what differs is what starts it,
 * not how it is written.
 */
@ActivityInterface
// @@@SNIPSTART samples-java-nexus-walkthrough-activities
public interface ApprovalActivities {

  /** STEP 4 placeholder - real logic would apply policy, check limits, or call a risk service. */
  @ActivityMethod
  void evaluateAutoDecision(String itemId, double amount);

  /** STEP 4 placeholder - real logic would page an approver or open a ticket. */
  @ActivityMethod
  void notifyApproverOfPendingRequest(String itemId, String requester);

  /**
   * STEP 9 - The Standalone Activity behind the notifyRequester Operation. One outbound
   * notification, no state, nothing to wait for. In this sample it only logs; real logic would call
   * an email provider, push to a notification service, or write to an outbox.
   */
  @ActivityMethod
  NotifyRequesterOutput notifyRequester(String requester, NotifyRequesterInput.Decision decision);
}
// @@@SNIPEND
