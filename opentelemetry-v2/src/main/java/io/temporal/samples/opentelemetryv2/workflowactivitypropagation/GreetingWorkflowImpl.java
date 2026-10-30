package io.temporal.samples.opentelemetryv2.workflowactivitypropagation;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

// @@@SNIPSTART samples-java-opentelemetry-v2-workflow-span
public class GreetingWorkflowImpl implements GreetingWorkflow {
  static final String INSTRUMENTATION_NAME =
      "io.temporal.samples.opentelemetryv2.workflowactivitypropagation";

  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String greet(String name) {
    Span span =
        GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME)
            .spanBuilder("workflow-operation")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return activities.composeGreeting(name);
    } finally {
      span.end();
    }
  }
}
// @@@SNIPEND
