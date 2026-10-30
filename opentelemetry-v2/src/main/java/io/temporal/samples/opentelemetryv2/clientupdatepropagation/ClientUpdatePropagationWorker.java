package io.temporal.samples.opentelemetryv2.clientupdatepropagation;

import io.temporal.client.WorkflowClient;
import io.temporal.opentelemetry.v2.OpenTelemetryPlugin;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.samples.opentelemetryv2.OpenTelemetrySetup;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class ClientUpdatePropagationWorker {
  private static final String SERVICE_NAME =
      "temporal-otel-v2-custom-client-update-propagation-worker";

  public static void main(String[] args) throws IOException {
    ReplaySafeOpenTelemetry openTelemetry =
        OpenTelemetrySetup.initializeGlobalOpenTelemetry(SERVICE_NAME);

    OpenTelemetryPlugin plugin = OpenTelemetryPlugin.newBuilder().build();

    WorkflowClient client = OpenTelemetrySetup.newClient(plugin);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(GreetingWorkflow.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

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
