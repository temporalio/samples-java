# Temporal Google Cloud OpenTelemetry worker

This sample runs a continuously polling Temporal worker in a **Cloud Run worker pool** and
exports Temporal SDK metrics and traces through a
[Google-Built OpenTelemetry Collector](https://cloud.google.com/stackdriver/docs/instrumentation/opentelemetry-collector-cloud-run)
sidecar.

It is intentionally not a Cloud Run function or request-driven Cloud Run service. Worker pools
keep CPU allocated while the Temporal worker performs continuous background polling.

```text
Temporal worker
    │ OTLP/gRPC, localhost:4317
    ▼
Google-Built OpenTelemetry Collector sidecar
    ├── metrics ──► Google Managed Service for Prometheus
    └── traces  ──► Telemetry (OTLP) API ──► Cloud Trace storage
```

The Java process uses `GcpOpenTelemetryPlugin`, which configures the Temporal SDK metrics scope,
tracing interceptors, OTLP exporters, and shutdown flushing. The plugin defaults to
`http://localhost:4317` and derives `service.name` from the Cloud Run-provided
`CLOUD_RUN_WORKER_POOL` environment variable. It reports and exports metrics every 60 seconds by
default, matching the upstream OpenTelemetry SDK default and the coordinated Temporal GCP plugin
default across Java, Go, and Python. This sample deliberately does not override that interval.

## Unreleased SDK dependency

At the time this sample was added, `io.temporal:temporal-gcp` had not been released. The Gradle
build and Dockerfile therefore use `1.37.0-SNAPSHOT` as an explicit placeholder. A normal build
from Maven Central and the production Docker build remain blocked until a version containing the
module is published. Do not replace the plugin with hand-written OpenTelemetry configuration; that
would stop this sample from exercising the supported SDK API.

You can compile and test against an unmodified local `sdk-java` checkout with Gradle composite
substitution. The sample-level plugin-default test requires the coordinated 60-second SDK change,
so an older checkout fails instead of silently testing a different cadence:

```bash
./gradlew \
  -PtemporalSdkPath=/path/to/sdk-java \
  :gcp-opentelemetry:test \
  :gcp-opentelemetry:installDist
```

Alternatively, after publishing all required `1.37.0-SNAPSHOT` SDK modules to Maven Local, add
`-PuseMavenLocal=true`. Once `temporal-gcp` is released, update the default
`temporalGcpVersion` in `build.gradle` and `TEMPORAL_GCP_VERSION` in `Dockerfile` to that released
version.

## Files

- `src/main/java/io/temporal/samples/gcp/GcpOpenTelemetryWorker.java` creates the plugin, client,
  and long-lived worker and performs a bounded shutdown on `SIGTERM`.
- `collector-config.yaml` adapts Google's Cloud Run collector configuration for cumulative
  Prometheus metrics and batched traces.
- `worker-pool.yaml` deploys the worker and collector as two containers sharing localhost and
  injects the collector configuration from Secret Manager.
- `Dockerfile` packages the Gradle application as the worker container.

## Metric cadence and collector batching

`GcpOpenTelemetryPlugin` defaults to a 60-second metric reporting and export interval. Applications
can still override it with `Builder.setMetricsReportInterval(...)`; custom intervals must remain
above Google Cloud's five-second minimum. When an application supplies its own `OpenTelemetry`
instance, it must configure that instance's metric-reader cadence separately.

Metric cadence and collector batching solve different problems. This collector does **not** put its
cumulative OTLP metrics through a batch processor: a forced shutdown flush can arrive immediately
after a periodic export, and a metric batch could combine both points for the same Prometheus time
series even when the periodic interval is much longer than the batch timeout. Managed Service for
Prometheus rejects that request as `Duplicate TimeSeries`.

The five-second ingestion minimum and metric batching are separate constraints. Meeting the
minimum does not make batching cumulative metrics safe during shutdown.

The dedicated `batch/traces` processor retains a five-second timeout because trace batching is
independently useful and does not have the cumulative-series collision behavior. Do not add
`batch/traces` to `metrics/otlp` or introduce another metric batch processor as a substitute for
choosing an application metric cadence.

## Required Google Cloud APIs

Enable these APIs in the project that hosts the worker pool:

```bash
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  iam.googleapis.com \
  cloudresourcemanager.googleapis.com \
  monitoring.googleapis.com \
  telemetry.googleapis.com \
  cloudtrace.googleapis.com \
  --project="$PROJECT_ID"
```

`monitoring.googleapis.com` is required for Google Managed Service for Prometheus ingestion.
Traces are sent using authenticated OTLP to `telemetry.googleapis.com`; the Cloud Trace API must
also be enabled or Google Cloud discards trace data received by the Telemetry API.
The IAM API is used to create the runtime service account. The declarative worker-pool replacement
workflow can require the Cloud Resource Manager API to resolve the target project.

The account enabling APIs needs `roles/serviceusage.serviceUsageAdmin` (or equivalent
permissions).

## IAM

Use a user-managed service account as the Cloud Run worker pool service identity. The collector
uses that identity through Application Default Credentials; do not set
`GOOGLE_APPLICATION_CREDENTIALS` in Cloud Run.

Grant the worker-pool service account:

- `roles/monitoring.metricWriter` on the telemetry project, for the
  `googlemanagedprometheus` exporter.
- `roles/telemetry.tracesWriter` on the telemetry project, for OTLP traces sent to the Telemetry
  API. `roles/cloudtrace.agent` also contains the write permission, but the narrower Telemetry role
  is preferred here.
- `roles/serviceusage.serviceUsageConsumer` on the quota project (the same project in this
  example).
- `roles/secretmanager.secretAccessor` on the collector-config and Temporal API-key secrets.

For example:

```bash
gcloud iam service-accounts create temporal-gcp-worker --project="$PROJECT_ID"

SERVICE_ACCOUNT="temporal-gcp-worker@${PROJECT_ID}.iam.gserviceaccount.com"
for ROLE in \
  roles/monitoring.metricWriter \
  roles/telemetry.tracesWriter \
  roles/serviceusage.serviceUsageConsumer \
  roles/secretmanager.secretAccessor
do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="$ROLE"
done
```

The deployer needs `roles/run.admin` (or the documented worker-pool deployment permissions) and
`roles/iam.serviceAccountUser` on this service account. Creating the service account, Artifact
Registry repository, secrets, and their IAM bindings also requires the corresponding administrative
permissions. The runtime service account does not need those administrative roles.

The collector configuration does not export OTLP logs, so it does not require
`roles/logging.logWriter`. Cloud Run still captures the worker and collector containers' stdout and
stderr through its platform logging.

## Build and deploy

The commands below assume an existing Temporal Cloud namespace and API key.

1. Create the secrets. Pin the API key to a numbered version in `worker-pool.yaml`; the collector
   configuration is also pinned to a numbered version and injected as `OTELCOL_CONFIG`.

   ```bash
   printf '%s' "$TEMPORAL_API_KEY" | \
     gcloud secrets create temporal-api-key --data-file=- --project="$PROJECT_ID"

   gcloud secrets create temporal-gcp-otel-config \
     --data-file=gcp-opentelemetry/collector-config.yaml \
     --project="$PROJECT_ID"
   ```

   If either secret already exists, add a version with `gcloud secrets versions add` instead.
   The manifest loads the collector YAML with `--config=env:OTELCOL_CONFIG`. This is intentional:
   secret-backed file volumes have been rejected by some Cloud Run worker-pool rollouts even where
   current documentation advertises support.

2. After `temporal-gcp` is released, create an Artifact Registry repository and build and push the
   worker image from the repository root:

   ```bash
   REGION=us-central1
   RELEASED_TEMPORAL_GCP_VERSION=REPLACE_AFTER_RELEASE
   IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/temporal-samples/gcp-opentelemetry:latest"

   gcloud artifacts repositories create temporal-samples \
     --repository-format=docker \
     --location="$REGION" \
     --project="$PROJECT_ID"
   gcloud auth configure-docker "${REGION}-docker.pkg.dev"

   docker build \
     -f gcp-opentelemetry/Dockerfile \
     --build-arg "TEMPORAL_GCP_VERSION=${RELEASED_TEMPORAL_GCP_VERSION}" \
     -t "$IMAGE" \
     .
   docker push "$IMAGE"
   ```

3. Edit the placeholders in `worker-pool.yaml`:

   - `PROJECT_ID` and `REGION`.
   - `NAMESPACE_ID.ACCOUNT_ID` and the matching Temporal Cloud address.
   - The Temporal API-key and collector-config secret versions if either is not version `1`.
   - Image tag, task queue, instance count, and container resources as appropriate.

4. Deploy the worker pool:

   ```bash
   gcloud run worker-pools replace gcp-opentelemetry/worker-pool.yaml \
     --dry-run \
     --project="$PROJECT_ID"

   gcloud run worker-pools replace gcp-opentelemetry/worker-pool.yaml \
     --project="$PROJECT_ID"
   ```

Worker pools use manual instance counts. This manifest starts one continuously allocated instance;
setting the count to zero disables the worker pool.

## Collector startup and health requirements

The collector is a required dependency, not an optional observability add-on:

- `run.googleapis.com/container-dependencies` declares that `worker` depends on `collector`.
- The collector enables `health_check` on `0.0.0.0:13133` and has a startup probe on `/`.
  Cloud Run worker pools do not supply a default startup probe. Without this probe Cloud Run can
  start the worker even when the collector failed to load its configuration.
- The collector configuration is a Secret Manager-backed environment variable loaded through the
  collector's `env` configuration provider. Keep the YAML below the Cloud Run secret-environment
  size limit; this sample configuration is intentionally small.
- The worker starts only after the collector startup probe succeeds. A liveness probe restarts the
  collector if it later becomes unhealthy.
- The OTLP receiver listens on `localhost:4317`, which is reachable by both containers because
  containers in a worker-pool instance share a network namespace.
- A successful health probe confirms that the collector is running and accepted its configuration;
  it does not prove that Google Cloud ingestion and IAM are working. Check collector logs for
  exporter errors and verify both signals after deployment.

If the collector is unavailable after startup, the Temporal worker continues processing work but
telemetry delivery can be delayed or lost. Treat collector liveness and exporter failures as
operational alerts.

## Generate and view telemetry

Start `GreetingWorkflow` on task queue `gcp-opentelemetry` with a single string argument. For
example, with an already configured Temporal CLI:

```bash
temporal workflow start \
  --workflow-id gcp-otel-greeting \
  --type GreetingWorkflow \
  --task-queue gcp-opentelemetry \
  --input '"Google Cloud"'
```

Temporal SDK metrics appear as Prometheus metrics in Cloud Monitoring. Traces appear in Trace
Explorer after passing through the Telemetry API. The OpenTelemetry service name defaults to the
value of `CLOUD_RUN_WORKER_POOL`; set `OTEL_SERVICE_NAME` on the worker container only if you need
an explicit override.

For end-to-end metric verification, observe at least one normal 60-second periodic export, then
terminate or replace a worker-pool revision to exercise the forced shutdown flush. Confirm that
both exports reach Managed Service for Prometheus and that the collector logs contain no
`Duplicate TimeSeries` rejection. A short smoke test that exercises only one of these paths is not
sufficient evidence for the cumulative-metric pipeline.

## Shutdown

Cloud Run sends `SIGTERM` and allows 10 seconds before `SIGKILL`. The shutdown hook reserves six
seconds for graceful worker shutdown, one second for forced shutdown if necessary, and two seconds
for the plugin's Temporal-metrics and OpenTelemetry flush before closing the service stubs. The
flush runs after worker termination so it includes telemetry emitted by finishing tasks.
Long-running Activities must still use heartbeats and cancellation handling so they can stop within
the platform shutdown window.
