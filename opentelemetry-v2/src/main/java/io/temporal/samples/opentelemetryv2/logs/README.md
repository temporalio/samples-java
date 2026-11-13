## Logs

This sample instruments a Workflow and an Activity to emit logs. Each log
record carries the trace ID and span ID of the span it was logged in.

It turns on Temporal SDK operation spans with `setAddTemporalSpans(true)` in
[`LogsWorker.java`](LogsWorker.java), so the logs have spans to join.

### Run the sample

1. Run a [Temporal Service](../../../../../../../../../README.md#running-samples).

2. Start the Worker:

```bash
./gradlew -q :opentelemetry-v2:execute -PmainClass=io.temporal.samples.opentelemetryv2.logs.LogsWorker
```

3. In another terminal, start the Workflow:

```bash
temporal workflow execute \
  --task-queue logs \
  --type GreetingWorkflow \
  --input '"Temporal"'
```

4. The Worker prints each log record with its trace ID and span ID:

```text
INFO 'Requesting greeting for Temporal' : ed5a47978eb89fb2f492172747a5ede9 996ada643cb7ee95 [scopeInfo: io.temporal.samples.opentelemetryv2.logs.GreetingWorkflowImpl:] {}
INFO 'Formatting greeting for Temporal' : ed5a47978eb89fb2f492172747a5ede9 17d77396b969503a [scopeInfo: io.temporal.samples.opentelemetryv2.logs.GreetingActivitiesImpl:] {}
```

   The IDs are different on every run. Within a run, both records share one
   trace ID, and each has its own span ID.

   The Worker also exports spans to Jaeger. If Jaeger is running, as in the
   [traces sample](../traces), search for your trace ID in the
   [Jaeger UI](http://127.0.0.1:16686). The Workflow log's span ID matches the
   `RunWorkflow:GreetingWorkflow` span, and the Activity log's span ID matches
   the `RunActivity:ComposeGreeting` span.
