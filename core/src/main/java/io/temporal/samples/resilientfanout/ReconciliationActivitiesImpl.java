package io.temporal.samples.resilientfanout;

public class ReconciliationActivitiesImpl implements ReconciliationActivities {

  @Override
  public void notifySupportTeam(String userId, String reason) {
    System.out.println("notifying support team about '" + userId + "': " + reason);
  }

  @Override
  public void recordFailedOnboarding(String userId, String reason) {
    System.out.println("recording failed onboarding audit entry for '" + userId + "': " + reason);
  }

  @Override
  public void releaseBillingHold(String userId) {
    System.out.println("releasing billing hold for '" + userId + "'");
  }
}
