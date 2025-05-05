package io.temporal.samples.excludefrominterceptor.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ForInterceptorActivities {
  void forInterceptorActivityOne(Object output);

  void forInterceptorActivityTwo(Object output);
}
