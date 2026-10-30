# OpenTelemetry v2

These samples use the
[OpenTelemetry v2 plugin](https://github.com/temporalio/sdk-java/tree/master/contrib/temporal-opentelemetry-v2)
for replay-safe tracing.

## Jaeger

These samples export traces through OpenTelemetry to
[Jaeger, an OpenTelemetry-compatible tracing backend](https://www.jaegertracing.io/docs/latest/#relationship-with-opentelemetry).
Jaeger visualizes each trace as a timeline showing how an operation moved
through your code.

## Samples

- [Automatic instrumentation](src/main/java/io/temporal/samples/opentelemetryv2/automaticinstrumentation)
- [Workflow-to-Activity propagation](src/main/java/io/temporal/samples/opentelemetryv2/workflowactivitypropagation)
- [Client-to-Update propagation](src/main/java/io/temporal/samples/opentelemetryv2/clientupdatepropagation)

Shared setup lives in
[`OpenTelemetrySetup.java`](src/main/java/io/temporal/samples/opentelemetryv2/OpenTelemetrySetup.java).
