package io.temporal.samples.cloudrunworkerid;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** Activity interface used by {@link GreetingWorkflow}. */
@ActivityInterface
public interface GreetingActivities {

  @ActivityMethod
  String composeGreeting(String name);
}
