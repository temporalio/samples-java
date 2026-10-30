package io.temporal.samples.opentelemetryv2.clientupdatepropagation;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.temporal.workflow.Workflow;

// @@@SNIPSTART samples-java-opentelemetry-v2-update-handler-span
public class GreetingWorkflowImpl implements GreetingWorkflow {
  static final String INSTRUMENTATION_NAME =
      "io.temporal.samples.opentelemetryv2.clientupdatepropagation";

  private String result;

  @Override
  public String run() {
    Workflow.await(() -> result != null && Workflow.isEveryHandlerFinished());
    return result;
  }

  @Override
  public String greet(String name) {
    Span span =
        GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME)
            .spanBuilder("handle-update")
            .startSpan();
    try {
      result = "Hello, " + name + "!";
      return result;
    } finally {
      span.end();
    }
  }

  @Override
  public void validateGreet(String name) {
    Span span =
        GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME)
            .spanBuilder("validate-update")
            .startSpan();
    try {
      if (name.isEmpty()) {
        throw new IllegalArgumentException("name cannot be empty");
      }
    } finally {
      span.end();
    }
  }
}
// @@@SNIPEND
