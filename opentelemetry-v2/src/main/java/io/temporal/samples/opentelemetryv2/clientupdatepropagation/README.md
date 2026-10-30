## Client-to-Update propagation

This sample creates a custom span on the client and then initiates an Update, propagating the trace through to the Workflow.

### Run the sample

1. Run a [Temporal Service](../../../../../../../../../README.md#running-samples).

2. Start Jaeger from the repository root:

```bash
docker compose -f opentelemetry-v2/docker-compose.yaml up -d
```

3. Start the Worker:

```bash
./gradlew -q :opentelemetry-v2:execute -PmainClass=io.temporal.samples.opentelemetryv2.clientupdatepropagation.ClientUpdatePropagationWorker
```

4. In another terminal, start the Workflow and send the Update:

```bash
./gradlew -q :opentelemetry-v2:execute -PmainClass=io.temporal.samples.opentelemetryv2.clientupdatepropagation.Starter
```

5. Inspect the `temporal-otel-v2-custom-client-update-propagation-client`
   service in the [Jaeger UI](http://127.0.0.1:16686). The `send-update`,
   `validate-update`, and `handle-update` spans show custom span context
   propagation from the Client to the Workflow Update.
