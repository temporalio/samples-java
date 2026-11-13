package io.temporal.samples.resilientfanout;

import io.temporal.failure.ApplicationFailure;

/**
 * Demo implementation. Two sentinel user ids let the sample be run end-to-end against each outcome
 * without a test harness:
 *
 * <ul>
 *   <li>{@code "user-critical-failure"} fails account creation (the critical branch).
 *   <li>{@code "user-degraded"} fails storage provisioning (a best-effort branch).
 *   <li>Any other id succeeds on every branch.
 * </ul>
 */
public class OnboardingActivitiesImpl implements OnboardingActivities {

  @Override
  public String createAccount(String userId) {
    if ("user-critical-failure".equals(userId)) {
      System.out.println("account creation failing for '" + userId + "'");
      throw ApplicationFailure.newNonRetryableFailure(
          "account service rejected the request", "AccountCreationFailure");
    }
    System.out.println("created account for '" + userId + "'");
    return "account-" + userId;
  }

  @Override
  public void provisionStorage(String userId) {
    if ("user-degraded".equals(userId)) {
      System.out.println("storage provisioning failing for '" + userId + "'");
      throw ApplicationFailure.newNonRetryableFailure(
          "storage quota service unavailable", "StorageProvisioningFailure");
    }
    System.out.println("provisioned storage for '" + userId + "'");
  }

  @Override
  public void sendWelcomeEmail(String userId) {
    System.out.println("sent welcome email to '" + userId + "'");
  }
}
