package io.temporal.samples.gcp.cloudrun;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
  String composeGreeting(String name);
}
