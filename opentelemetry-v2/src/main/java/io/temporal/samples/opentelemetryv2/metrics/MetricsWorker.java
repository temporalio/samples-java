package io.temporal.samples.opentelemetryv2.metrics;

import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.ScopeCloseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.temporal.client.WorkflowClient;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MetricsWorker {
  static final String TASK_QUEUE = "metrics";
  private static final String SERVICE_NAME = "temporal-otel-v2-metrics-worker";

  public static void main(String[] args) throws IOException {
    // Serves recorded metrics in Prometheus format at http://127.0.0.1:9090/metrics.
    PrometheusHttpServer prometheus =
        PrometheusHttpServer.builder().setHost("127.0.0.1").setPort(9090).build();

    // Meters from this global drop recordings made while a Workflow replays.
    ReplaySafeOpenTelemetry openTelemetry =
        ReplaySafeOpenTelemetry.newBuilder()
            .setMeterProviderBuilder(
                SdkMeterProvider.builder()
                    .registerMetricReader(prometheus)
                    .setResource(
                        Resource.getDefault().toBuilder()
                            .put("service.name", SERVICE_NAME)
                            .build()))
            .build();
    GlobalOpenTelemetry.set(openTelemetry);

    // The SDK reports its own metrics through Tally. Micrometer bridges them into OpenTelemetry,
    // so they are served next to the custom counters.
    MeterRegistry registry = OpenTelemetryMeterRegistry.builder(openTelemetry).build();
    Scope scope =
        new RootScopeBuilder()
            .reporter(new MicrometerClientStatsReporter(registry))
            .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));

    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder(profile.toWorkflowServiceStubsOptions())
                // Exports Temporal SDK metrics, such as temporal_workflow_completed.
                // Comment out this line to turn them off and keep only the custom counters.
                .setMetricsScope(scope)
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
                  factory.awaitTermination(10, TimeUnit.SECONDS);
                  // Closing the scope sends its last report before OpenTelemetry closes.
                  try {
                    scope.close();
                  } catch (ScopeCloseException e) {
                    throw new RuntimeException(e);
                  }
                  openTelemetry.close();
                }));
    factory.start();
  }
}
