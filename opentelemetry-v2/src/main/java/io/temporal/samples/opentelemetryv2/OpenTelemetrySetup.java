package io.temporal.samples.opentelemetryv2;

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
import java.io.IOException;

public final class OpenTelemetrySetup {
  private OpenTelemetrySetup() {}

  /**
   * Registers the replay-safe OpenTelemetry instance that the OpenTelemetry v2 plugin requires as
   * the global.
   */
  // @@@SNIPSTART samples-java-opentelemetry-v2-tracer-provider
  public static ReplaySafeOpenTelemetry initializeGlobalOpenTelemetry(String serviceName) {
    OtlpGrpcSpanExporter exporter =
        OtlpGrpcSpanExporter.builder().setEndpoint("http://127.0.0.1:4317").build();

    ReplaySafeOpenTelemetry openTelemetry =
        ReplaySafeOpenTelemetry.newBuilder()
            .setTracerProviderBuilder(
                SdkTracerProvider.builder()
                    // BatchSpanProcessor performs exporter I/O outside the Workflow thread.
                    .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                    .setResource(
                        Resource.getDefault().toBuilder().put("service.name", serviceName).build()))
            .build();
    GlobalOpenTelemetry.set(openTelemetry);
    return openTelemetry;
  }

  // @@@SNIPEND

  /** Creates a client whose service stubs carry the plugin, so its Workers get it too. */
  // @@@SNIPSTART samples-java-opentelemetry-v2-plugin-client
  public static WorkflowClient newClient(OpenTelemetryPlugin plugin) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder(profile.toWorkflowServiceStubsOptions())
                .setPlugins(plugin)
                .build());
    return WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }
  // @@@SNIPEND
}
