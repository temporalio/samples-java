package io.temporal.samples.gcp.cloudrun.opentelemetry;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
  String composeGreeting(String name);
}
