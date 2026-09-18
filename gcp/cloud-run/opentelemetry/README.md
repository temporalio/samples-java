# Temporal Cloud Run OpenTelemetry worker

A Temporal Worker running in a Google Cloud Run **worker pool** that exports
Temporal SDK metrics and traces through a
[Google-Built OpenTelemetry Collector](https://cloud.google.com/stackdriver/docs/instrumentation/opentelemetry-collector-cloud-run)
sidecar. Worker pools keep CPU allocated, unlike request-driven Cloud Run services.

`CloudRunOpenTelemetryPlugin` from `io.temporal:temporal-gcp-cloud-run-opentelemetry` configures the
SDK metrics scope, tracing interceptors, OTLP exporters (default `http://localhost:4317`), and
shutdown flushing. It derives `service.name` from `CLOUD_RUN_WORKER_POOL` and reports metrics every
60 seconds, matching the OpenTelemetry SDK default across the Temporal SDKs.

## Unreleased SDK dependency

`temporal-gcp-cloud-run-opentelemetry` is not yet released. `settings.gradle` resolves it (and the
other `io.temporal:*` modules) from a local SDK checkout via a Gradle composite build, defaulting to
`../sdk-java` and overridable with `-PtemporalSdkPath`. CI has no checkout, so its build stays red
until the module ships; then drop the composite block and bump `javaSDKVersion`.

```bash
./gradlew -PtemporalSdkPath=/path/to/sdk-java :gcp:cloud-run:opentelemetry:build
```

## Files

- `.../cloudrun/opentelemetry/CloudRunWorker.java` — plugin, client, Temporal Worker, bounded `SIGTERM` shutdown.
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
     --data-file=gcp/cloud-run/opentelemetry/collector-config.yaml --project="$PROJECT_ID"
   ```

2. Build and push the image (tag `REGION-docker.pkg.dev/PROJECT_ID/temporal-samples/cloud-run-worker:latest`)
   with `docker build -f gcp/cloud-run/opentelemetry/Dockerfile .`.

3. Replace the placeholders in `worker-pool.yaml` (project, region, namespace, address, secret
   versions, image), then deploy:

   ```bash
   gcloud run worker-pools replace gcp/cloud-run/opentelemetry/worker-pool.yaml --project="$PROJECT_ID"
   ```

The worker-pool service account needs `roles/monitoring.metricWriter`, `roles/telemetry.tracesWriter`,
and `roles/secretmanager.secretAccessor`. Start a workflow on task queue `cloud-run-worker` to
generate telemetry:

```bash
temporal workflow start --type GreetingWorkflow --task-queue cloud-run-worker \
  --workflow-id cloud-run-greeting --input '"Google Cloud"'
```

The collector does **not** batch cumulative metrics: a shutdown flush batched with a recent periodic
export would collide on the same Prometheus series and be rejected as `Duplicate TimeSeries`.
