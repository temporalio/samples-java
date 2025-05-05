package io.temporal.samples.excludefrominterceptor.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface MyActivities {
  void activityOne(String input);

  void activityTwo(String input);
}
