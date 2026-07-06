## Standalone Nexus Operations

> [!WARNING]
> Standalone Nexus operations are experimental and may be subject to backwards-incompatible
> changes. They require a Temporal server that implements and enables them via the dynamic configs
> shown below.
> 
This sample shows how to invoke and manage **standalone Nexus operations** — Nexus operations
started directly by a client rather than from within a caller workflow. The long-running operation
(`startGreeting`) is backed by a `GreetingWorkflow` that blocks until it is cancelled or terminated;
the quick operation (`greet`) is synchronous and completes immediately.

`StandaloneClientStarter` runs each capability in turn:
1. **Execute** an operation and read its result, both directly (`execute`) and via a handle
   (`start` then `handle.getResult`).
2. **Cancel** a running operation (`handle.cancel`).
3. **Terminate** a running operation (`handle.terminate`). Operation-terminate is a known gap that
   does not stop the backing workflow, so the sample also terminates the backing workflow by ID.
4. **Visibility** — `list` operations with a status filter and `count` them (total and grouped) via
   `NexusClient`.

### Running

Start a Temporal server (version `1.7.2-standalone-nexus-operations`) with the standalone-Nexus dynamic configs enabled:

```bash
temporal server start-dev \
  --dynamic-config-value nexusoperation.enableStandalone=true \
  --dynamic-config-value history.enableChasmCallbacks=true
```

Create the namespace and the Nexus endpoint:

```bash
temporal operator nexus endpoint create \
  --name nexus-standalone-operation-endpoint \
  --target-namespace default \
  --target-task-queue nexusstandalone-handler-task-queue
```

In one terminal, start the handler worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandalone.handler.HandlerWorker
```

In a second terminal, run the starter:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandalone.StandaloneClientStarter
```

Expected output (operation IDs and Visibility counts will differ between runs):

```
execute() returned: Hello, execute!
start() id=73e77105-f7ec-4a1f-a24a-1f9a9cc87248 then getResult() returned: Hello, execute-via-handle!
Started 'to-cancel' id=12b554b5-d9f8-4f4f-9314-db508fd91999, requesting cancellation
Operation id=12b554b5-d9f8-4f4f-9314-db508fd91999 ended as expected after cancel: Nexus operation failed: operationId='12b554b5-d9f8-4f4f-9314-db508fd91999'
Started 'to-terminate' id=b1dae9d4-2d6b-45d6-ab3b-8725cc2cf6de, terminating
'to-terminate' ended as expected after terminate: Nexus operation failed: operationId='b1dae9d4-2d6b-45d6-ab3b-8725cc2cf6de'
Terminated backing workflow greeting-to-terminate-ef71547a
List filtered to Completed returned 2 operation(s)
Total operation count: 4
Grouped count total=4, groups:
  group values=[[Canceled]] count=1
  group values=[[Completed]] count=2
  group values=[[Terminated]] count=1
```

### Cancellation vs. termination

A workflow-backed Nexus operation does **not** need any explicit cancel handling to be cancellable.
When you call `handle.cancel(...)`, the server delivers a cancellation request to the backing
workflow, which makes the blocking call (`Workflow.await` in `GreetingWorkflowImpl`) throw a
`CanceledFailure`; letting it propagate out of the workflow method ends both the workflow and the
operation as cancelled. Cancellation is **cooperative**, though: if the backing workflow caught and
ignored `CanceledFailure` (or did all of its waiting inside a detached cancellation scope), the
cancel request would have no effect and the operation would run until it completes or hits its
schedule-to-close timeout.

`handle.terminate(...)` is different. It forcefully closes the **operation** record, but currently
does **not** propagate to the backing workflow (a known gap) — the workflow keeps running and
nothing appears in its history. Until that gap is closed, terminate the backing workflow directly by
its workflow ID, as `StandaloneClientStarter.terminateBackingWorkflow` does.
