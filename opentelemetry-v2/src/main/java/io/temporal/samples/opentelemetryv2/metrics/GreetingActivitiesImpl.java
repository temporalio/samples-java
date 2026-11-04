package io.temporal.samples.opentelemetryv2.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;

public class GreetingActivitiesImpl implements GreetingActivities {
  @Override
  public String composeGreeting(String name) {
    GlobalOpenTelemetry.getMeter(GreetingWorkflowImpl.INSTRUMENTATION_NAME)
        .counterBuilder("greeting.activity.runs")
        .setDescription("Number of times the greeting Activity has run")
        .setUnit("{run}")
        .build()
        .add(1);
    return "Hello, " + name + "!";
  }
}
