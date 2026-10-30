package io.temporal.samples.opentelemetryv2.workflowactivitypropagation;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
  String composeGreeting(String name);
}
