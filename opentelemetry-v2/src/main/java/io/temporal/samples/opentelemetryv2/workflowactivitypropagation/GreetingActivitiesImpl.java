package io.temporal.samples.opentelemetryv2.workflowactivitypropagation;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;

// @@@SNIPSTART samples-java-opentelemetry-v2-activity-span
public class GreetingActivitiesImpl implements GreetingActivities {
  @Override
  public String composeGreeting(String name) {
    Span span =
        GlobalOpenTelemetry.getTracer(GreetingWorkflowImpl.INSTRUMENTATION_NAME)
            .spanBuilder("activity-operation")
            .startSpan();
    try {
      return "Hello, " + name + "!";
    } finally {
      span.end();
    }
  }
}
// @@@SNIPEND
