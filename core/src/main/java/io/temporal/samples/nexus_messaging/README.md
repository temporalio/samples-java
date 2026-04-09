This sample shows how to expose a long-running workflow's queries, updates, and signals as Nexus
operations. The caller interacts only with the Nexus service; the workflow is a private
implementation detail.

There are **two caller patterns** that share the same handler workflow (`GreetingWorkflow`):

| | `caller/` (entity pattern) | `caller_remote/` (remote-start pattern) |
|---|---|---|
| **Who creates the workflow?** | The handler worker starts it on boot | The caller starts it via a `runFromRemote` Nexus operation |
| **Who knows the workflow ID?** | Only the handler | The caller chooses and passes it in every operation |
| **Nexus service** | `NexusGreetingService` — inputs carry only business data | `NexusRemoteGreetingService` — every input includes a `workflowId` |
| **When to use** | Single shared entity; callers don't need lifecycle control | Caller needs to create and target specific workflow instances |

### Directory structure

- `service/` — shared Nexus service definitions (`NexusGreetingService`, `NexusRemoteGreetingService`) and `Language` enum
- `handler/` — `GreetingWorkflow` and its implementation, `GreetingActivity`, both Nexus service impls (`NexusGreetingServiceImpl`, `NexusRemoteGreetingServiceImpl`), and the handler worker
- `caller/` — entity-pattern caller (workflow, worker, starter)
- `caller_remote/` — remote-start caller (workflow, worker, starter)

### Running

Start a Temporal server:

```bash
temporal server start-dev
```
Create the namespaces and Nexus endpoint:

```bash
temporal operator namespace create --namespace nexus-messaging-handler-namespace
temporal operator namespace create --namespace nexus-messaging-caller-namespace

temporal operator nexus endpoint create \
  --name nexus-messaging-nexus-endpoint \
  --target-namespace nexus-messaging-handler-namespace \
  --target-task-queue nexus-messaging-handler-task-queue
```

In one terminal, start the handler worker (shared by both patterns):

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.handler.HandlerWorker --args="-target-host localhost:7233 -namespace my-target-namespace"
```

#### Entity pattern

In the second terminal, run the caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.caller.CallerWorker --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

In the third terminal, start the caller workflow:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.caller.CallerStarter --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

Expected output:

```
supported languages: [CHINESE, ENGLISH]
language changed: ENGLISH -> ARABIC
workflow approved
```

#### Remote-start pattern

In a second terminal, run the remote caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.caller_remote.CallerRemoteWorker --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

In a third terminal, start the remote caller workflow:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.caller_remote.CallerRemoteStarter --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

Expected output:

```
started remote greeting workflow: nexus-messaging-remote-greeting-workflow
supported languages: [CHINESE, ENGLISH]
language changed: ENGLISH -> ARABIC
workflow approved
workflow result: مرحبا بالعالم
```

### How it works

#### The handler (shared by both patterns)

`GreetingWorkflow` is a long-running "entity" workflow that holds the current language and a map of
greetings. It exposes its state through standard Temporal primitives:

- `getLanguages` / `getLanguage` — `@QueryMethod`s for reading state
- `setLanguage` — an `@UpdateMethod` for switching between already-loaded languages
- `setLanguageUsingActivity` — an `@UpdateMethod` that calls an activity to fetch a greeting for
  a language not yet in the map (uses `WorkflowLock` to serialize concurrent activity calls)
- `approve` — a `@SignalMethod` that lets the workflow complete

The workflow waits until approved and all in-flight update handlers have finished, then returns the
greeting in the current language.

Both Nexus service implementations translate incoming Nexus operations into calls against
`GreetingWorkflow` stubs — queries, updates, and signals. The caller never interacts with the
workflow directly.

#### Entity pattern (`caller/` + `NexusGreetingService`)

The handler worker starts a single `GreetingWorkflow` on boot with a fixed workflow ID.
`NexusGreetingServiceImpl` holds that workflow ID in its constructor and routes every operation to
it. The caller's inputs contain only business data (language, name), not workflow IDs.

`CallerWorkflowImpl` creates a `NexusGreetingService` stub and:
1. Queries for supported languages (`getLanguages` — backed by a `@QueryMethod`)
2. Changes the language to Arabic (`setLanguage` — backed by an `@UpdateMethod` that calls an activity)
3. Confirms the change via a second query (`getLanguage`)
4. Approves the workflow (`approve` — backed by a `@SignalMethod`)

#### Remote-start pattern (`caller_remote/` + `NexusRemoteGreetingService`)

No workflow is pre-started. Instead, `NexusRemoteGreetingService` adds a `runFromRemote` operation
that starts a new `GreetingWorkflow` with a caller-chosen workflow ID using
`WorkflowRunOperation`. Every other operation also includes the `workflowId` in its input so that
`NexusRemoteGreetingServiceImpl` can look up the right workflow stub.

`CallerRemoteWorkflowImpl` creates a `NexusRemoteGreetingService` stub and:
1. Starts a remote `GreetingWorkflow` via `runFromRemote` and waits for it to be running
2. Queries, updates, and approves that workflow — same operations as the entity pattern, but each
   input carries the workflow ID
3. Waits for the remote workflow to complete and returns its result (the greeting string)

This pattern is useful when the caller needs to control the lifecycle of individual workflow
instances rather than sharing a single entity.
