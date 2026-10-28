# OpenTelemetry for ADK Workflows and Activities

Trace the Temporal Workflow and its model/tool Activities with OpenTelemetry. The example uses the
SDK's tracing interceptor through its OpenTracing bridge, an instance-scoped OpenTelemetry SDK,
and a replay-aware exporter. It exports Temporal operation spans without installing a global ADK
telemetry provider. Native ADK agent-internal spans still lack replay suppression in plugin 0.1.0.

From the repository root, start `temporal server start-dev` in one terminal. Start a Worker with
console span export in another:

```bash
ADK_MODEL_MODE=offline ADK_TRACING=console ./gradlew :googleadk:worker
```

For live Gemini, stop the offline Worker and restart it with:

```bash
GOOGLE_API_KEY=your_key ADK_MODEL_MODE=live ADK_TRACING=console ./gradlew :googleadk:worker
```

In a third terminal:

```bash
temporal workflow execute --type ObservabilityWorkflow --task-queue google-adk-samples --input '"What is the weather in Chicago?"'
```

Offline mode returns `Chicago is sunny.`. The Worker logs a `RunWorkflow:ObservabilityWorkflow`
span, two `RunActivity:googleadk.InvokeModel` spans, and one `RunActivity:GetWeather` span, together
with their scheduling spans. W3C trace context links these operations into one trace. The weather
Activity returns illustrative data and a host timestamp.

`SampleTelemetry` accepts a standard `SpanExporter`; replace the console exporter to send spans to
an OpenTelemetry backend. Keep its `ReplayAwareSpanExporter` in the `SimpleSpanProcessor` so replay
is checked on the Workflow thread before any asynchronous export. Query spans remain observable.
The exporter suppresses spans ended during replay. As with Temporal Activity execution, a real
Activity retry exports another attempt; traces do not promise exactly-once execution.

The tests use an in-memory exporter and a scripted model, requiring no keys, telemetry service, or
external Temporal Service. They verify model/tool spans and trace linkage, failed/retried tool
attempts, and replay without additional exported spans:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.ObservabilityWorkflowTest
```
