## Standalone Nexus Operations

> [!WARNING]
> Standalone Nexus operations are experimental and may be subject to backwards-incompatible
> changes. They require a Temporal server that implements and enables them via the dynamic configs
> shown below. Use the dev server build at
> https://github.com/temporalio/cli/releases/tag/v1.7.4-standalone-nexus-operations.
>
This sample shows how to invoke and manage **standalone Nexus operations** — Nexus operations
started directly by a client rather than from within a caller workflow. The long-running operation
(`startGreeting`) is backed by a `GreetingWorkflow` that blocks until it is cancelled; the quick
operation (`greet`) is synchronous and completes immediately.

`StandaloneClientStarter` runs each capability in turn:
1. **Execute** an operation and read its result, both directly (`execute`) and via a handle
   (`getHandle` then `handle.getResult`).
2. **Cancel** a running operation (`handle.cancel`).
3. **Visibility** — `list` operations with a status filter and `count` them (total and grouped) via
   `NexusClient`.

The starter and worker connect to two different namespaces (a "caller" namespace and a "handler"
namespace) — this mirrors how Nexus is typically used to cross namespace boundaries. The client is
configured via the SDK's [environment configuration](https://docs.temporal.io/develop/environment-configuration)
support (`ClientConfigProfile.load()`), which reads `TEMPORAL_NAMESPACE`, `TEMPORAL_ADDRESS`, etc.
from the environment (and optionally a profile from `temporal.toml`).

### Run locally against a dev server

1. Start the [Temporal dev server build that supports standalone Nexus operations](https://docs.temporal.io/standalone-nexus-operation#temporal-cli-support) with the required namespaces pre-created:

   ```bash
   ./temporal server start-dev \
     --namespace my-caller-namespace \
     --namespace my-handler-namespace
   ```

2. Create a Nexus endpoint that routes to the handler namespace and the worker's task queue:

   ```bash
   ./temporal operator nexus endpoint create \
     --name my-nexus-endpoint \
     --target-namespace my-handler-namespace \
     --target-task-queue nexus-handler-queue
   ```

3. In a second terminal, start the handler worker in the handler namespace:

   ```bash
   TEMPORAL_NAMESPACE=my-handler-namespace \
     ./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandalone.handler.HandlerWorker
   ```

4. In a third terminal, run the starter in the caller namespace:

   ```bash
   TEMPORAL_NAMESPACE=my-caller-namespace \
     ./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandalone.StandaloneClientStarter
   ```

Expected output (operation IDs are fixed, but Visibility counts grow across runs):

```
execute() returned: Hello, execute!
getHandle(id=execute-nexus) then getResult() returned: Hello, execute!
Started 'to-cancel' id=start-and-cancel-nexus, requesting cancellation
Operation id=start-and-cancel-nexus ended as expected after cancel: Nexus operation failed: operationId='start-and-cancel-nexus'
List filtered to Completed returned 1 operation(s)
Total operation count: 2
Grouped count total=2, groups:
  group values=[[Canceled]] count=1
  group values=[[Completed]] count=1
```

### Run against Temporal Cloud

1. Create two namespaces in Temporal Cloud (for example `my-caller-namespace.<account>` and
   `my-handler-namespace.<account>`) and generate an API key (or mTLS cert) that can access both.

2. Create a Nexus endpoint that targets the handler namespace and the worker's task queue. See the
   Temporal Cloud instructions at https://docs.temporal.io/nexus/registry#create-a-nexus-endpoint.
   Use:
   - Endpoint name: `my-nexus-endpoint`
   - Target namespace: `my-handler-namespace.<account>`
   - Target task queue: `nexus-handler-queue`
   - Allowed caller namespaces: include `my-caller-namespace.<account>` (endpoints reject callers
     that are not on this list)

3. Add two profiles to your [environment configuration file](https://docs.temporal.io/develop/environment-configuration),
   one per namespace. Using API keys:

   ```toml
   [profile.handler]
   address = "<region>.<cloud>.api.temporal.io:7233"
   namespace = "my-handler-namespace.<account>"
   api_key = "<your-api-key>"

   [profile.caller]
   address = "<region>.<cloud>.api.temporal.io:7233"
   namespace = "my-caller-namespace.<account>"
   api_key = "<your-api-key>"
   ```

   For mTLS instead of API keys, set `tls.client_cert_path` and `tls.client_key_path` on each profile
   (see the [docs](https://docs.temporal.io/develop/environment-configuration) for the full schema).

4. Run the worker and starter in separate terminals, selecting the appropriate profile in each:

   ```bash
   # terminal 1 (worker, handler namespace)
   TEMPORAL_PROFILE=handler \
     ./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandalone.handler.HandlerWorker
   ```

   ```bash
   # terminal 2 (starter, caller namespace)
   TEMPORAL_PROFILE=caller \
     ./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandalone.StandaloneClientStarter
   ```

### Cancellation

A workflow-backed Nexus operation does **not** need any explicit cancel handling to be cancellable.
When you call `handle.cancel(...)`, the server delivers a cancellation request to the backing
workflow, which makes the blocking call (`Workflow.await` in `GreetingWorkflowImpl`) throw a
`CanceledFailure`; letting it propagate out of the workflow method ends both the workflow and the
operation as cancelled. Cancellation is **cooperative**, though: if the backing workflow caught and
ignored `CanceledFailure` (or did all of its waiting inside a detached cancellation scope), the
cancel request would have no effect and the operation would run until it completes or hits its
schedule-to-close timeout.
