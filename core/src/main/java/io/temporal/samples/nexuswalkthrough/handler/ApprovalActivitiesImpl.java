package io.temporal.samples.nexuswalkthrough.handler;

import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Placeholder implementations. See {@link ApprovalActivities} for what each one stands in for. */
public class ApprovalActivitiesImpl implements ApprovalActivities {

  private static final Logger logger = LoggerFactory.getLogger(ApprovalActivitiesImpl.class);

  @Override
  public void evaluateAutoDecision(String itemId, double amount) {
    logger.info("Evaluating auto-decision rules for {} at {}", itemId, amount);
  }

  @Override
  public void notifyApproverOfPendingRequest(String itemId, String requester) {
    logger.info("Approver notified that {} from {} is waiting", itemId, requester);
  }

  @Override
  public NotifyRequesterOutput notifyRequester(
      String requester, NotifyRequesterInput.Decision decision) {
    logger.info("Notifying {} that their request was {}", requester, decision.getValue());
    return new NotifyRequesterOutput(requester);
  }
}
