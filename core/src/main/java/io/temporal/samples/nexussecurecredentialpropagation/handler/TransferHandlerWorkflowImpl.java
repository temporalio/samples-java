package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TransferHandlerWorkflowImpl implements TransferHandlerWorkflow {
  @Override
  public TransferService.TransferOutput settle(Settlement settlement) {
    // Stand-in for settling the payment with a downstream system, which would receive the same
    // business resource ID as its idempotency key.
    Workflow.sleep(Duration.ofSeconds(5));
    return new TransferService.TransferOutput(
        settlement.getPaymentId(), "SETTLED", settlement.getApprovedFor());
  }
}
