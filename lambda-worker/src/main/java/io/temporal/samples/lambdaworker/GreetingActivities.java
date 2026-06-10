package io.temporal.samples.lambdaworker;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** Activity interface used by the sample workflow. */
@ActivityInterface
public interface GreetingActivities {

  @ActivityMethod
  String createGreeting(String name);
}
