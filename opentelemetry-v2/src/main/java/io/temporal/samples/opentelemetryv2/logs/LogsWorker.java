package io.temporal.samples.opentelemetryv2.logs;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
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

public class LogsWorker {
  static final String TASK_QUEUE = "logs";
  private static final String SERVICE_NAME = "temporal-otel-v2-logs-worker";

  public static void main(String[] args) throws IOException {
    Resource resource = Resource.getDefault().toBuilder().put("service.name", SERVICE_NAME).build();

    // Log records are printed to the console with the trace and span IDs of the span that was
    // current when they were logged. Spans also go to Jaeger, where you can look up a trace ID.
    // Batch processors perform exporter I/O outside the Workflow thread.
    ReplaySafeOpenTelemetry openTelemetry =
        ReplaySafeOpenTelemetry.newBuilder()
            .setTracerProviderBuilder(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        BatchSpanProcessor.builder(
                                OtlpGrpcSpanExporter.builder()
                                    .setEndpoint("http://127.0.0.1:4317")
                                    .build())
                            .build())
                    .setResource(resource))
            .setLoggerProviderBuilder(
                SdkLoggerProvider.builder()
                    .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(SystemOutLogRecordExporter.create())
                            .build())
                    .setResource(resource))
            .build();
    GlobalOpenTelemetry.set(openTelemetry);

    // Sends SLF4J logs from this package to OpenTelemetry. See logback.xml.
    OpenTelemetryAppender.install(openTelemetry);

    OpenTelemetryPlugin plugin =
        OpenTelemetryPlugin.newBuilder()
            // Adds Temporal SDK operation spans. Log records take the trace and span IDs of the
            // span that is current when they are logged.
            .setAddTemporalSpans(true)
            .build();

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
