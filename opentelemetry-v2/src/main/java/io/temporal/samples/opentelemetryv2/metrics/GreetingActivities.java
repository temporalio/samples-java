package io.temporal.samples.opentelemetryv2.metrics;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
  String composeGreeting(String name);
}
