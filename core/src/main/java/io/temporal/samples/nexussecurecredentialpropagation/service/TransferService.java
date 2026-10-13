package io.temporal.samples.nexussecurecredentialpropagation.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

/** A Nexus service whose operation runs on behalf of an end user. */
@Service
public interface TransferService {
  String NAME = "TransferService";
  String TRANSFER = "transfer";

  /** Intent constraint: the exact payment ID the user approved. */
  String CONSTRAINT_PAYMENT_ID = "paymentId";

  /** Intent constraint: the exact beneficiary the user approved. */
  String CONSTRAINT_BENEFICIARY = "beneficiary";

  /** Intent constraint: the maximum amount, in cents, the user approved. */
  String CONSTRAINT_MAX_AMOUNT_CENTS = "maxAmountCents";

  /** Scope the end user's token must carry. */
  String REQUIRED_SCOPE = "payments:transfer";

  class TransferInput {
    private final String tenantId;
    private final String userId;
    private final String paymentId;
    private final String beneficiary;
    private final long amountCents;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TransferInput(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("userId") String userId,
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("beneficiary") String beneficiary,
        @JsonProperty("amountCents") long amountCents) {
      this.tenantId = tenantId;
      this.userId = userId;
      this.paymentId = paymentId;
      this.beneficiary = beneficiary;
      this.amountCents = amountCents;
    }

    @JsonProperty("tenantId")
    public String getTenantId() {
      return tenantId;
    }

    /** The user the transfer is for. Verified against the token subject, never trusted alone. */
    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }

    /** Business identity of the transfer, used for idempotency. */
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
  }

  class TransferOutput {
    private final String paymentId;
    private final String status;
    private final String approvedFor;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TransferOutput(
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("status") String status,
        @JsonProperty("approvedFor") String approvedFor) {
      this.paymentId = paymentId;
      this.status = status;
      this.approvedFor = approvedFor;
    }

    @JsonProperty("paymentId")
    public String getPaymentId() {
      return paymentId;
    }

    @JsonProperty("status")
    public String getStatus() {
      return status;
    }

    /** Subject of the verified principal the transfer ran for. */
    @JsonProperty("approvedFor")
    public String getApprovedFor() {
      return approvedFor;
    }
  }

  @Operation
  TransferOutput transfer(TransferInput input);
}
