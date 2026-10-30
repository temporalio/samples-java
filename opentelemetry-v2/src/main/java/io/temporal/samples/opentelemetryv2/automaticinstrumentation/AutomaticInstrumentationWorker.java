package io.temporal.samples.opentelemetryv2.automaticinstrumentation;

import io.temporal.client.WorkflowClient;
import io.temporal.opentelemetry.v2.OpenTelemetryPlugin;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.samples.opentelemetryv2.OpenTelemetrySetup;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class AutomaticInstrumentationWorker {
  private static final String SERVICE_NAME = "temporal-otel-v2-automatic-worker";

  public static void main(String[] args) throws IOException {
    ReplaySafeOpenTelemetry openTelemetry =
        OpenTelemetrySetup.initializeGlobalOpenTelemetry(SERVICE_NAME);

    // @@@SNIPSTART samples-java-opentelemetry-v2-temporal-spans
    OpenTelemetryPlugin plugin = OpenTelemetryPlugin.newBuilder().setAddTemporalSpans(true).build();
    // @@@SNIPEND

    WorkflowClient client = OpenTelemetrySetup.newClient(plugin);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(GreetingWorkflow.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  factory.shutdown();
                  openTelemetry.close();
                }));
    factory.start();
  }
}
