package io.temporal.samples.earlyreturn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TxResult {
  private final String transactionId;
  private final String status;

  // Jackson-compatible constructor with @JsonCreator and @JsonProperty annotations
  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public TxResult(
      @JsonProperty("transactionId") String transactionId, @JsonProperty("status") String status) {
    this.transactionId = transactionId;
    this.status = status;
  }

  @JsonProperty("transactionId")
  public String getTransactionId() {
    return transactionId;
  }

  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return String.format("InitResult{transactionId='%s', status='%s'}", transactionId, status);
  }
}
