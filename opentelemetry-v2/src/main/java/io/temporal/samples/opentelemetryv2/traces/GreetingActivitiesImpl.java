package io.temporal.samples.opentelemetryv2.traces;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public class GreetingActivitiesImpl implements GreetingActivities {
  @Override
  public String composeGreeting(String name) {
    Span span =
        GlobalOpenTelemetry.getTracer(GreetingWorkflowImpl.INSTRUMENTATION_NAME)
            .spanBuilder("format-greeting")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return "Hello, " + name + "!";
    } finally {
      span.end();
    }
  }
}
