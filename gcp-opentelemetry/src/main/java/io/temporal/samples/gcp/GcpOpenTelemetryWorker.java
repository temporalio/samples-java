package io.temporal.samples.gcp;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.gcp.GcpOpenTelemetryPlugin;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** A continuously polling Temporal worker for a Cloud Run worker pool. */
public final class GcpOpenTelemetryWorker {
  public static final String DEFAULT_TASK_QUEUE = "gcp-opentelemetry";

  private GcpOpenTelemetryWorker() {}

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    GcpOpenTelemetryPlugin telemetryPlugin = GcpOpenTelemetryPlugin.newBuilder().build();

    WorkflowServiceStubsOptions serviceOptions =
        WorkflowServiceStubsOptions.newBuilder(profile.toWorkflowServiceStubsOptions())
            .setPlugins(telemetryPlugin)
            .build();
    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(serviceOptions);
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
    WorkerFactory factory = WorkerFactory.newInstance(client);

    String taskQueue = taskQueue();
    Worker worker = factory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> shutdown(factory, service, telemetryPlugin), "temporal-worker-shutdown"));

    factory.start();
    System.out.printf(
        "Temporal worker started: taskQueue=%s, otelEndpoint=%s, serviceName=%s%n",
        taskQueue, telemetryPlugin.getEndpoint(), telemetryPlugin.getServiceName());

    // Cloud Run worker pools are continuous workloads. Keep the process alive until SIGTERM.
    factory.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  private static String taskQueue() {
    String configured = System.getenv("TEMPORAL_TASK_QUEUE");
    return configured == null || configured.trim().isEmpty() ? DEFAULT_TASK_QUEUE : configured;
  }

  private static void shutdown(
      WorkerFactory factory, WorkflowServiceStubs service, GcpOpenTelemetryPlugin telemetryPlugin) {
    // Cloud Run allows 10 seconds between SIGTERM and SIGKILL. Flush only after the asynchronous
    // worker shutdown so telemetry produced by finishing tasks is included.
    factory.shutdown();
    factory.awaitTermination(6, TimeUnit.SECONDS);
    if (!factory.isTerminated()) {
      factory.shutdownNow();
      factory.awaitTermination(1, TimeUnit.SECONDS);
    }
    telemetryPlugin.newFlushHook().run(Duration.ofSeconds(2));
    service.shutdown();
  }
}
