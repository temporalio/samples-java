package io.temporal.samples.resilientfanout;

import io.temporal.activity.ActivityInterface;

/** Activities backing each onboarding branch. */
@ActivityInterface
public interface OnboardingActivities {

  /** Critical: onboarding cannot succeed without an account. */
  String createAccount(String userId);

  /** Best-effort: storage can be provisioned later out-of-band if this fails. */
  void provisionStorage(String userId);

  /** Best-effort: a missed welcome email is not worth failing onboarding over. */
  void sendWelcomeEmail(String userId);
}
