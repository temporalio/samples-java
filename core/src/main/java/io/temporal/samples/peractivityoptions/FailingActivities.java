

package io.temporal.samples.peractivityoptions;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FailingActivities {
  void activityTypeA();

  void activityTypeB();
}
