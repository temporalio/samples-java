## Metrics

This sample instruments a Workflow and an Activity to emit metrics.

By default, it also exports Temporal SDK metrics with `setMetricsScope(scope)`
in [`MetricsWorker.java`](MetricsWorker.java). Comment out that line to turn
them off and keep only the custom counters.


### Run the sample

1. Run a [Temporal Service](../../../../../../../../../README.md#running-samples).

2. Start the Worker:

```bash
./gradlew -q :opentelemetry-v2:execute -PmainClass=io.temporal.samples.opentelemetryv2.metrics.MetricsWorker
```

3. In another terminal, start the Workflow:

```bash
temporal workflow execute \
  --task-queue metrics \
  --type GreetingWorkflow \
  --input '"Temporal"'
```

4. Open the [metrics endpoint](http://127.0.0.1:9090/metrics). It shows the
   custom `greeting_workflow_runs_total` and `greeting_activity_runs_total`
   counters. With SDK metrics on, it also shows Temporal SDK metrics, such as
   `temporal_workflow_completed_total` and
   `temporal_activity_execution_latency_seconds`.

```text
greeting_activity_runs_total{otel_scope_name="io.temporal.samples.opentelemetryv2.metrics"} 1.0
greeting_workflow_runs_total{otel_scope_name="io.temporal.samples.opentelemetryv2.metrics"} 1.0
```
