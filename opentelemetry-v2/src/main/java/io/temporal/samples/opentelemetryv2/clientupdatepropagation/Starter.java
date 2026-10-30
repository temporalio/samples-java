package io.temporal.samples.opentelemetryv2.clientupdatepropagation;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.opentelemetry.v2.OpenTelemetryPlugin;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.samples.opentelemetryv2.OpenTelemetrySetup;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Starter {
  private static final String INSTRUMENTATION_NAME =
      "io.temporal.samples.opentelemetryv2.clientupdatepropagation.Starter";
  private static final String SERVICE_NAME =
      "temporal-otel-v2-custom-client-update-propagation-client";
  private static final Logger log = LoggerFactory.getLogger(Starter.class);

  public static void main(String[] args) throws IOException {
    try (ReplaySafeOpenTelemetry ignored =
        OpenTelemetrySetup.initializeGlobalOpenTelemetry(SERVICE_NAME)) {
      WorkflowClient client =
          OpenTelemetrySetup.newClient(OpenTelemetryPlugin.newBuilder().build());
      GreetingWorkflow workflow =
          client.newWorkflowStub(
              GreetingWorkflow.class,
              WorkflowOptions.newBuilder().setTaskQueue(GreetingWorkflow.TASK_QUEUE).build());
      WorkflowClient.start(workflow::run);

      log.info("Update result: {}", sendUpdate(workflow, "Temporal"));
      log.info("Workflow result: {}", WorkflowStub.fromTyped(workflow).getResult(String.class));
      client.getWorkflowServiceStubs().shutdown();
    }
  }

  // @@@SNIPSTART samples-java-opentelemetry-v2-client-update
  private static String sendUpdate(GreetingWorkflow workflow, String name) {
    Span span =
        GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME).spanBuilder("send-update").startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return workflow.greet(name);
    } finally {
      span.end();
    }
  }
  // @@@SNIPEND
}
