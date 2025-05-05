

package io.temporal.samples.peractivityoptions;

import io.temporal.activity.Activity;

public class FailingActivitiesImpl implements FailingActivities {
  @Override
  public void activityTypeA() {
    // Get the activity type
    String type = Activity.getExecutionContext().getInfo().getActivityType();
    // Get the retry attempt
    int attempt = Activity.getExecutionContext().getInfo().getAttempt();
    // Wrap checked exception and throw
    throw Activity.wrap(
        new NullPointerException("Activity type: " + type + " attempt times: " + attempt));
  }

  @Override
  public void activityTypeB() {
    // Get the activity type
    String type = Activity.getExecutionContext().getInfo().getActivityType();
    // Get the retry attempt
    int attempt = Activity.getExecutionContext().getInfo().getAttempt();
    // Wrap checked exception and throw
    throw Activity.wrap(
        new NullPointerException("Activity type: " + type + " attempt times: " + attempt));
  }
}
