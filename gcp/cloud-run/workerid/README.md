# Temporal Cloud Run worker-identity sample

A continuously polling Temporal Java worker for a Google Cloud Run **worker pool** that registers
`CloudRunIdPlugin` from `io.temporal:temporal-gcp-cloud-run-id` on the client, so the worker identity
is derived from Cloud Run instance metadata as `{instanceId}@{revision}`. The plugin sets identity
only. A small greeting workflow and activity run until Cloud Run stops the instance.

> Google Cloud Run support is experimental and may change without notice.

## Unreleased SDK dependency

`temporal-gcp-cloud-run-id` is not yet released. `settings.gradle` resolves it (and the other
`io.temporal:*` modules) from a local SDK checkout via a Gradle composite build, defaulting to
`../sdk-java` and overridable with `-PtemporalSdkPath`. CI has no checkout, so its build stays red
until the module ships; then drop the composite block and bump `javaSDKVersion`.

## How it works

Cloud Run worker pools set `CLOUD_RUN_WORKER_POOL` and `CLOUD_RUN_REVISION` (services set `K_SERVICE`
and `K_REVISION`). `CloudRunIdPlugin` reads those plus the instance id from the Cloud Run metadata
server and sets the client identity to `{instanceId}@{revision}` unless one is already set; workers
created from the client inherit it. `GoogleCloudRunMetadata.fetch().identity()` exposes the same
value, which the worker logs at startup.

The worker reads `TEMPORAL_ADDRESS` (default `127.0.0.1:7233`), `TEMPORAL_NAMESPACE` (default
`default`), and `TEMPORAL_TASK_QUEUE` (default `cloud-run-worker-id`). A plaintext connection is used;
configure TLS or an API key in `CloudRunWorker.java` for a secured Service such as Temporal Cloud.

## Build and test

```bash
./gradlew :gcp:cloud-run:workerid:test
./gradlew -PtemporalSdkPath=/path/to/sdk-java :gcp:cloud-run:workerid:installDist
```

## Deploy

Worker pools keep CPU allocated for continuous polling. Until `temporal-gcp-cloud-run-id` is released
a remote `--source` build cannot resolve it, so build the image locally against your SDK checkout and
deploy it by tag:

```bash
export REGION=us-central1
gcloud run worker-pools deploy cloud-run-worker-id \
  --image "$REGION-docker.pkg.dev/$PROJECT_ID/<repo>/cloud-run-worker-id:latest" \
  --region "$REGION" \
  --set-env-vars "TEMPORAL_ADDRESS=<addr>,TEMPORAL_NAMESPACE=<ns>,TEMPORAL_TASK_QUEUE=cloud-run-worker-id"
```

Each revision starts a fresh instance whose worker reports a distinct identity.

## Start a workflow

```bash
temporal workflow start --type GreetingWorkflow --task-queue cloud-run-worker-id \
  --workflow-id cloud-run-greeting --input '"Cloud Run"'
```

The identity appears on the task-queue pollers (`temporal task-queue describe`) and recorded events.
Delete the pool with `gcloud run worker-pools delete cloud-run-worker-id --region "$REGION"`.
