package io.temporal.samples.resilientfanout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of a successful onboarding run. {@code degradedReasons} is non-empty when one or more
 * best-effort branches failed but the run still completed because the critical branch succeeded.
 */
public class OnboardingResult {

  private String accountId;
  private List<String> degradedReasons;

  // Default constructor for the JSON payload converter (Workflow results cross the wire).
  public OnboardingResult() {
    this.degradedReasons = new ArrayList<>();
  }

  public OnboardingResult(String accountId, List<String> degradedReasons) {
    this.accountId = accountId;
    this.degradedReasons = Collections.unmodifiableList(degradedReasons);
  }

  public String getAccountId() {
    return accountId;
  }

  public List<String> getDegradedReasons() {
    return degradedReasons;
  }

  @JsonIgnore
  public boolean isDegraded() {
    return !degradedReasons.isEmpty();
  }

  @Override
  public String toString() {
    return "OnboardingResult{accountId='"
        + accountId
        + "', degradedReasons="
        + degradedReasons
        + '}';
  }
}
