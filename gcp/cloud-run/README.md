# Temporal Cloud Run worker

A Temporal Worker running in a Google Cloud Run **worker pool** that uses both GCP Cloud Run
plugins together. Worker pools keep CPU allocated, unlike request-driven Cloud Run services. Both
plugins are registered on the service stubs, so they propagate to the client and to workers created
from it.

- `CloudRunIdPlugin` (`io.temporal:temporal-gcp-cloud-run-id`) sets the Temporal client identity
  from Cloud Run instance metadata as `{instanceId}@{revision}`, unless an identity is already set.
- `CloudRunOpenTelemetryPlugin` (`io.temporal:temporal-gcp-cloud-run-opentelemetry`) configures the
  SDK metrics scope, tracing interceptors, OTLP exporters (default `http://localhost:4317`), and
  shutdown flushing, exporting to a
  [Google-Built OpenTelemetry Collector](https://cloud.google.com/stackdriver/docs/instrumentation/opentelemetry-collector-cloud-run)
  sidecar. It derives `service.name` from `CLOUD_RUN_WORKER_POOL` and reports metrics every 60
  seconds, matching the OpenTelemetry SDK default across the Temporal SDKs.

> Google Cloud Run support is experimental and may change without notice.

## Build

`temporal-gcp-cloud-run-id` and `temporal-gcp-cloud-run-opentelemetry` are published on Maven Central.

```bash
./gradlew :gcp:cloud-run:build
```

## Files

- `.../cloudrun/CloudRunWorker.java` — both plugins, client, Temporal Worker, bounded `SIGTERM` shutdown.
- `collector-config.yaml` — collector for cumulative Prometheus metrics and batched traces.
- `worker-pool.yaml` — worker and collector containers sharing localhost, config from Secret Manager.
- `Dockerfile` — packages the Gradle application as the worker container.

## Deploy

Run from the repository root, with a Temporal Cloud namespace and API key.

1. Store the API key and collector config in Secret Manager:

   ```bash
   printf '%s' "$TEMPORAL_API_KEY" | \
     gcloud secrets create temporal-api-key --data-file=- --project="$PROJECT_ID"
   gcloud secrets create temporal-collector-config \
     --data-file=gcp/cloud-run/collector-config.yaml --project="$PROJECT_ID"
   ```

2. Create the Artifact Registry repo, then build and push the image:

   ```bash
   gcloud artifacts repositories create temporal-samples --repository-format=docker \
     --location="$REGION" --project="$PROJECT_ID"
   IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/temporal-samples/cloud-run-worker:latest"
   docker build -f gcp/cloud-run/Dockerfile -t "$IMAGE" . && docker push "$IMAGE"
   ```

3. Replace the placeholders in `worker-pool.yaml`, then deploy:

   ```bash
   gcloud run worker-pools replace gcp/cloud-run/worker-pool.yaml --project="$PROJECT_ID"
   ```

The service account needs `roles/monitoring.metricWriter`, `roles/telemetry.tracesWriter`, and
`roles/secretmanager.secretAccessor`. Start a workflow on task queue `cloud-run-worker`:

```bash
temporal workflow execute --type GreetingWorkflow --task-queue cloud-run-worker \
  --workflow-id cloud-run-greeting --input '"Google Cloud"'
```

It prints `Hello Google Cloud!`, confirming the deployed worker ran the task.

The collector does **not** batch cumulative metrics: a shutdown flush batched with a recent periodic
export would collide on the same Prometheus series and be rejected as `Duplicate TimeSeries`.
