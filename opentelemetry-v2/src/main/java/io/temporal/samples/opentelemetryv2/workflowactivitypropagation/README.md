## Workflow-to-Activity propagation

This sample propagates a replay-safe custom Workflow span to a custom Activity
span.

### Run the sample

1. Run a [Temporal Service](../../../../../../../../../README.md#running-samples).

2. Start Jaeger from the repository root:

```bash
docker compose -f opentelemetry-v2/docker-compose.yaml up -d
```

3. Start the Worker:

```bash
./gradlew -q :opentelemetry-v2:execute -PmainClass=io.temporal.samples.opentelemetryv2.workflowactivitypropagation.WorkflowActivityPropagationWorker
```

4. In another terminal, start the Workflow:

```bash
temporal workflow execute \
  --task-queue workflow-activity-propagation \
  --type GreetingWorkflow \
  --input '"Temporal"'
```

5. Inspect the
   `temporal-otel-v2-custom-workflow-activity-propagation-worker` service in the
   [Jaeger UI](http://127.0.0.1:16686). The `workflow-operation` and
   `activity-operation` spans show custom span context propagation from the
   Workflow to the Activity.
