## Automatic instrumentation

This sample enables Temporal SDK operation spans on the Worker.

### Run the sample

1. Run a [Temporal Service](../../../../../../../../../README.md#running-samples).

2. Start Jaeger from the repository root:

```bash
docker compose -f opentelemetry-v2/docker-compose.yaml up -d
```

3. Start the Worker:

```bash
./gradlew -q :opentelemetry-v2:execute -PmainClass=io.temporal.samples.opentelemetryv2.automaticinstrumentation.AutomaticInstrumentationWorker
```

4. In another terminal, start the Workflow:

```bash
temporal workflow execute \
  --task-queue automatic-instrumentation \
  --type GreetingWorkflow \
  --input '"Temporal"'
```

5. Inspect the `temporal-otel-v2-automatic-worker` service in the
   [Jaeger UI](http://127.0.0.1:16686). The plugin automatically creates
   Temporal SDK operation spans (`RunWorkflow:GreetingWorkflow`,
   `StartActivity:ComposeGreeting`, and `RunActivity:ComposeGreeting`).
