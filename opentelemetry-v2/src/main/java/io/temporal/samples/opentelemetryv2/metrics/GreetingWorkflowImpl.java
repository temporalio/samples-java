package io.temporal.samples.opentelemetryv2.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class GreetingWorkflowImpl implements GreetingWorkflow {
  static final String INSTRUMENTATION_NAME = "io.temporal.samples.opentelemetryv2.metrics";

  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String greet(String name) {
    GlobalOpenTelemetry.getMeter(INSTRUMENTATION_NAME)
        .counterBuilder("greeting.workflow.runs")
        .setDescription("Number of times the greeting Workflow has run")
        .setUnit("{run}")
        .build()
        .add(1);
    return activities.composeGreeting(name);
  }
}
