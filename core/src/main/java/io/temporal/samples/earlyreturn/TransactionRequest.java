

package io.temporal.samples.earlyreturn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TransactionRequest {
  private final String sourceAccount;
  private final String targetAccount;
  private final int amount;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public TransactionRequest(
      @JsonProperty("sourceAccount") String sourceAccount,
      @JsonProperty("targetAccount") String targetAccount,
      @JsonProperty("amount") int amount) {
    this.sourceAccount = sourceAccount;
    this.targetAccount = targetAccount;
    this.amount = amount;
  }

  @JsonProperty("sourceAccount")
  public String getSourceAccount() {
    return sourceAccount;
  }

  @JsonProperty("targetAccount")
  public String getTargetAccount() {
    return targetAccount;
  }

  @JsonProperty("amount")
  public int getAmount() {
    return amount;
  }

  @Override
  public String toString() {
    return String.format(
        "TransactionRequest{sourceAccount='%s', targetAccount='%s', amount=%d}",
        sourceAccount, targetAccount, amount);
  }
}
