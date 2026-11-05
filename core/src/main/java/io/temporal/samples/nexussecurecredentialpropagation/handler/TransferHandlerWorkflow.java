package io.temporal.samples.nexussecurecredentialpropagation.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TransferHandlerWorkflow {
  /**
   * Only the minimal authorized identifiers reach the backing Workflow. The token and its claims
   * never enter the handler's history.
   */
  class Settlement {
    private final String paymentId;
    private final String beneficiary;
    private final long amountCents;
    private final String approvedFor;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Settlement(
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("beneficiary") String beneficiary,
        @JsonProperty("amountCents") long amountCents,
        @JsonProperty("approvedFor") String approvedFor) {
      this.paymentId = paymentId;
      this.beneficiary = beneficiary;
      this.amountCents = amountCents;
      this.approvedFor = approvedFor;
    }

    @JsonProperty("paymentId")
    public String getPaymentId() {
      return paymentId;
    }

    @JsonProperty("beneficiary")
    public String getBeneficiary() {
      return beneficiary;
    }

    @JsonProperty("amountCents")
    public long getAmountCents() {
      return amountCents;
    }

    @JsonProperty("approvedFor")
    public String getApprovedFor() {
      return approvedFor;
    }
  }

  @WorkflowMethod
  TransferService.TransferOutput settle(Settlement settlement);
}
