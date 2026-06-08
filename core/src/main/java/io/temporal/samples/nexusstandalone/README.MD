## Standalone Nexus Operations

This sample shows how to invoke and manage **standalone Nexus operations** — Nexus operations
started directly by a client rather than from within a caller workflow. The long-running operation
(`startGreeting`) is backed by a `GreetingWorkflow` that blocks until it is cancelled or terminated;
the quick operation (`greet`) is synchronous and completes immediately.

`StandaloneClientStarter` runs each capability in turn:
1. **Execute** an operation and read its result — synchronously (`execute`) and asynchronously
   (`executeAsync`).
2. **Cancel** a running operation (`handle.cancel`).
3. **Terminate** a running operation (`handle.terminate`). Operation-terminate is a known gap that
   does not stop the backing workflow, so the sample also terminates the backing workflow by ID.
4. **Visibility** — `list` operations with a status filter and `count` them (total and grouped) via
   `NexusClient`.
5. **Client options and interceptors** — set the identity and data converter, and register two
   logging interceptors.

> [!WARNING]
> Standalone Nexus operations are experimental and may be subject to backwards-incompatible
> changes. They require a Temporal server that implements and enables them via the dynamic configs
> shown below.

### Running

Start a Temporal server with the standalone-Nexus dynamic configs enabled:

```bash
temporal server start-dev \
  --dynamic-config-value nexusoperation.enableStandalone=true \
  --dynamic-config-value history.enableChasmCallbacks=true
```

Create the namespace and the Nexus endpoint:

```bash
temporal operator namespace create --namespace default

temporal operator nexus endpoint create \
  --name nexusstandalone-endpoint \
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
executeAsync() returned: Hello, executeAsync!
Started 'to-cancel' id=<uuid>, requesting cancellation
Operation id=<uuid> final status: NEXUS_OPERATION_EXECUTION_STATUS_CANCELED
Started 'to-terminate' id=<uuid>, terminating
Final status of 'to-terminate': NEXUS_OPERATION_EXECUTION_STATUS_TERMINATED
Terminated backing workflow greeting-to-terminate-<runId>
List filtered to Completed returned 2 operation(s)
Total operation count: 4
Grouped count total=4, groups:
  group values=[[Canceled]] count=1
  group values=[[Completed]] count=2
  group values=[[Terminated]] count=1
[interceptor second] -> startNexusOperationExecution
[interceptor first] -> startNexusOperationExecution
[interceptor first] <- startNexusOperationExecution
[interceptor second] <- startNexusOperationExecution
Result through interceptor chain: Hello, interceptors!
```

The four interceptor lines come from a single operation: `execute()` issues one
`startNexusOperationExecution` call that passes through both interceptors. The last-registered
interceptor is outermost, so the call flows in `second → first → root` and back out `first → second`,
and each interceptor logs once on the way in and once on the way out.

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
