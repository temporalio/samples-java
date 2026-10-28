package io.temporal.samples.googleadk.observability;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.opentracing.OpenTracingSpanContextCodec;
import io.temporal.opentracing.OpenTracingWorkerInterceptor;
import io.temporal.worker.WorkerFactoryOptions;

public final class SampleTelemetry implements AutoCloseable {
  private final OpenTelemetrySdk telemetry;
  private final OpenTracingOptions options;

  // @@@SNIPSTART java-google-adk-observability-setup
  public SampleTelemetry(SpanExporter exporter) {
    telemetry =
        OpenTelemetrySdk.builder()
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        SimpleSpanProcessor.create(new ReplayAwareSpanExporter(exporter)))
                    .build())
            .build();
    options =
        OpenTracingOptions.newBuilder()
            .setTracer(OpenTracingShim.createTracerShim(telemetry))
            .setSpanContextCodec(OpenTracingSpanContextCodec.TEXT_MAP_CODEC)
            .build();
  }

  public WorkerFactoryOptions workerOptions() {
    return WorkerFactoryOptions.newBuilder()
        .setWorkerInterceptors(new OpenTracingWorkerInterceptor(options))
        .build();
  }

  // @@@SNIPEND

  public static SampleTelemetry console() {
    return new SampleTelemetry(LoggingSpanExporter.create());
  }

  @Override
  public void close() {
    telemetry.close();
  }
}
