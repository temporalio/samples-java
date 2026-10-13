package io.temporal.samples.resilientfanout;

import io.temporal.activity.ActivityInterface;

/**
 * Independent, unrelated side effects run when onboarding fails. Each one talks to a different
 * downstream system. They are unrelated to each other on purpose: one failing must not prevent the
 * others from being attempted. See {@link OnboardingWorkflowImpl#reconcileFailure} for how this is
 * achieved with {@code Saga}.
 */
@ActivityInterface
public interface ReconciliationActivities {

  void notifySupportTeam(String userId, String reason);

  void recordFailedOnboarding(String userId, String reason);

  void releaseBillingHold(String userId);
}
