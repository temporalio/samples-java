package io.temporal.samples.opentelemetryv2.traces;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.opentelemetry.v2.OpenTelemetryPlugin;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class TracesWorker {
  static final String TASK_QUEUE = "traces";
  private static final String SERVICE_NAME = "temporal-otel-v2-traces-worker";

  public static void main(String[] args) throws IOException {
    OtlpGrpcSpanExporter exporter =
        OtlpGrpcSpanExporter.builder().setEndpoint("http://127.0.0.1:4317").build();

    // The plugin requires a replay-safe OpenTelemetry instance registered as the global.
    ReplaySafeOpenTelemetry openTelemetry =
        ReplaySafeOpenTelemetry.newBuilder()
            .setTracerProviderBuilder(
                SdkTracerProvider.builder()
                    // BatchSpanProcessor performs exporter I/O outside the Workflow thread.
                    .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                    .setResource(
                        Resource.getDefault().toBuilder()
                            .put("service.name", SERVICE_NAME)
                            .build()))
            .build();
    GlobalOpenTelemetry.set(openTelemetry);

    OpenTelemetryPlugin plugin =
        OpenTelemetryPlugin.newBuilder()
            // Adds Temporal SDK operation spans, such as RunWorkflow and RunActivity.
            // Comment out this line to turn them off and keep only the custom spans.
            .setAddTemporalSpans(true)
            .build();

    // Service stubs that carry the plugin pass it to their client and Workers.
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder(profile.toWorkflowServiceStubsOptions())
                .setPlugins(plugin)
                .build());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
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
