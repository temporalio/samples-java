# Temporal Cloud Run worker-identity worker

This sample runs a continuously polling Temporal Java Worker in a Google Cloud Run **worker pool**.
It registers the `WorkerIdPlugin` from the `temporal-gcp-cloud-run-worker-id` module on the Temporal
client so the Worker's Temporal identity is derived from Cloud Run instance metadata as
`{instanceId}@{revision}`. It registers a small greeting Workflow and Activity and runs until Cloud
Run stops the instance. Identity only: the plugin sets the worker identity and nothing else.

Cloud Run runs a long-lived container rather than a per-request handler, so there is no function to
wrap: registering the plugin on the client fetches the metadata once at startup and applies the
derived identity to the client and the Workers created from it.

> Experimental: Google Cloud Run support is experimental and may change without notice.

## Unreleased SDK dependency

This sample depends on `io.temporal:temporal-gcp-cloud-run-worker-id`, which is **not yet released**
to Maven Central. Until it ships, the samples build wires the module from a local Temporal Java SDK
checkout through a Gradle composite build (`includeBuild`), configured in the samples root
`settings.gradle`.

- It defaults to a sibling `../sdk-java-2` checkout on the `cloud-run-worker-id` branch.
- Override the location with `-PtemporalSdkPath=/path/to/sdk-java`.
- When that checkout is absent, the composite build is skipped and only this module is affected; the
  other samples still build.

Once `temporal-gcp-cloud-run-worker-id` is released, remove the composite-build block from
`settings.gradle` and bump `javaSDKVersion` in the samples root `build.gradle` to the released
version; the standard Maven Central build then works without the local checkout. This sample's pull
request stays a draft until then.

## Prerequisites

- Java 17+
- The Temporal CLI (to start Workflows)
- The Google Cloud CLI (`gcloud`) with a project that has Cloud Run enabled
- A Temporal Service reachable from Cloud Run. A plaintext connection is used by default; configure
  TLS or an API key in `CloudRunWorker.java` for a secured Service such as Temporal Cloud.

## Files

- `src/main/java/io/temporal/samples/gcp/cloudrun/workerid/CloudRunWorker.java` fetches the Cloud
  Run metadata, registers `WorkerIdPlugin` on the client to apply the derived identity, and runs a
  long-lived Worker with a bounded shutdown on `SIGTERM`.
- `GreetingWorkflow` / `GreetingWorkflowImpl` and `GreetingActivities` / `GreetingActivitiesImpl` are
  the sample Workflow and Activity.
- `Dockerfile` packages the Gradle application as the Worker container.

## How it works

Cloud Run **worker pools** set `CLOUD_RUN_WORKER_POOL` and `CLOUD_RUN_REVISION` on every instance
(Cloud Run **services** set `K_SERVICE` and `K_REVISION`). `GoogleCloudRunMetadata.fetch()` resolves:

- **name**: the first non-empty of `CLOUD_RUN_WORKER_POOL` then `K_SERVICE`.
- **revision**: the first non-empty of `CLOUD_RUN_REVISION` then `K_REVISION`.
- **instance id**: a single HTTP `GET` to the Cloud Run metadata server
  (`http://metadata.google.internal/computeMetadata/v1/instance/id`, header `Metadata-Flavor:
  Google`).

`WorkerIdPlugin`, registered on the client with `WorkflowClientOptions.Builder.setPlugins(...)`, then
sets the Worker identity to `{instanceId}@{revision}` (falling back to `{instanceId}@{name}` and then
`{instanceId}`) unless an identity is already set. Workers created from the client inherit that
identity; the plugin sets nothing else on them.

The Worker reads its connection settings from the environment:

```bash
TEMPORAL_ADDRESS     # host:port of the Temporal frontend (default 127.0.0.1:7233)
TEMPORAL_NAMESPACE   # Temporal Namespace (default "default")
TEMPORAL_TASK_QUEUE  # Task Queue to poll (default "cloud-run-worker-id")
```

`CLOUD_RUN_WORKER_POOL` and `CLOUD_RUN_REVISION` are injected by Cloud Run and do not need to be set
manually.

## Build and test locally

The unit test uses `TestWorkflowRule` and needs neither Cloud Run nor a running Temporal Service:

```bash
./gradlew :gcp:cloud-run:workerid:test
```

Build the runnable application (from a local SDK checkout, per the note above):

```bash
./gradlew -PtemporalSdkPath=/path/to/sdk-java :gcp:cloud-run:workerid:installDist
```

## Deploy to a Cloud Run worker pool

Worker pools keep CPU allocated so the Temporal Worker can poll continuously; they are not
request-driven Cloud Run services. Set your connection values and deploy from the sample directory:

```bash
export REGION=us-central1
export TEMPORAL_ADDRESS=<your-namespace>.<account>.tmprl.cloud:7233
export TEMPORAL_NAMESPACE=<your-namespace>.<account>
export TEMPORAL_TASK_QUEUE=cloud-run-worker-id

gcloud run worker-pools deploy cloud-run-worker-id \
  --source . \
  --region "$REGION" \
  --set-env-vars "TEMPORAL_ADDRESS=$TEMPORAL_ADDRESS,TEMPORAL_NAMESPACE=$TEMPORAL_NAMESPACE,TEMPORAL_TASK_QUEUE=$TEMPORAL_TASK_QUEUE"
```

`--source .` builds the container from the included `Dockerfile`. Because the image build resolves
the unreleased `temporal-gcp-cloud-run-worker-id` module, a remote source build succeeds only once
that module is released (or published to your Maven Local and made available to the build). Until
then, build the image locally against your SDK checkout and deploy it with `--image` instead:

```bash
gcloud run worker-pools deploy cloud-run-worker-id \
  --image "$REGION-docker.pkg.dev/$PROJECT_ID/<repo>/cloud-run-worker-id:latest" \
  --region "$REGION" \
  --set-env-vars "TEMPORAL_ADDRESS=$TEMPORAL_ADDRESS,TEMPORAL_NAMESPACE=$TEMPORAL_NAMESPACE,TEMPORAL_TASK_QUEUE=$TEMPORAL_TASK_QUEUE"
```

Each Cloud Run revision starts a fresh instance whose Worker reports a distinct identity, which the
Worker logs at startup.

## Start a Workflow

After the Worker is polling, start the sample Workflow on the same Task Queue:

```bash
temporal workflow start \
  --task-queue cloud-run-worker-id \
  --type GreetingWorkflow \
  --workflow-id cloud-run-greeting \
  --input '"Cloud Run"'
```

The Worker's identity appears on its Task Queue pollers (for example in `temporal task-queue
describe`) and on the events it records.

## Shutdown

Cloud Run sends `SIGTERM` and allows a short grace period before `SIGKILL`. The shutdown hook stops
polling, waits up to six seconds for in-flight tasks to drain, escalates to a forced shutdown if
needed, and then closes the service connection. Long-running Activities should still heartbeat and
handle cancellation so they can stop within the platform's shutdown window.

## Clean up

Delete the worker pool when you are done:

```bash
gcloud run worker-pools delete cloud-run-worker-id --region "$REGION"
```
