package io.temporal.samples.earlyreturn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Transaction {
  private final String id;
  private final String sourceAccount;
  private final String targetAccount;
  private final int amount;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public Transaction(
      @JsonProperty("id") String id,
      @JsonProperty("sourceAccount") String sourceAccount,
      @JsonProperty("targetAccount") String targetAccount,
      @JsonProperty("amount") int amount) {
    this.id = id;
    this.sourceAccount = sourceAccount;
    this.targetAccount = targetAccount;
    this.amount = amount;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
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
        "Transaction{id='%s', sourceAccount='%s', targetAccount='%s', amount=%d}",
        id, sourceAccount, targetAccount, amount);
  }
}
