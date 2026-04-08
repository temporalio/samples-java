This sample shows how to create a Nexus service that is backed by a long-running workflow and
exposes operations that execute updates and queries against that workflow. The long-running
workflow, and the updates/queries, are private implementation details of the Nexus service: the
caller does not know how the operations are implemented.

This is a Java port of the
[nexus_sync_operations Python sample](https://github.com/temporalio/samples-python/tree/main/nexus_sync_operations).

### Sample directory structure

- [`service/GreetingService.java`](./service/GreetingService.java) — shared Nexus service definition with input/output types
- [`service/Language.java`](./service/Language.java) — shared language enum
- [`handler/`](./handler/) — Nexus operation handlers, the long-running entity workflow they back, an activity, and a handler worker
- [`caller/`](./caller/) — a caller workflow that executes Nexus operations, together with a worker and starter

### Instructions

Start a Temporal server:

```bash
temporal server start-dev
```

Create the caller and handler namespaces and the Nexus endpoint:

```bash
temporal operator namespace create --namespace nexus-sync-operations-handler-namespace
temporal operator namespace create --namespace nexus-sync-operations-caller-namespace

temporal operator nexus endpoint create \
  --name nexus-sync-operations-nexus-endpoint \
  --target-namespace nexus-sync-operations-handler-namespace \
  --target-task-queue nexus-sync-operations-handler-task-queue
```

In one terminal, run the handler worker (starts the long-running entity workflow and polls for
Nexus, workflow, and activity tasks):

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_sync_operations.handler.HandlerWorker
```

In a second terminal, run the caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_sync_operations.caller.CallerWorker
```

In a third terminal, start the caller workflow:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_sync_operations.caller.CallerStarter
```

You should see output like:

```
supported languages: [CHINESE, ENGLISH]
language changed: ENGLISH -> ARABIC
```

### How it works

The handler starts a single long-running `GreetingWorkflow` entity workflow when the worker boots.
This workflow holds the current language and a map of greetings, and exposes:

- `getLanguages` — a `@QueryMethod` listing supported languages
- `getLanguage` — a `@QueryMethod` returning the current language
- `setLanguage` — an `@UpdateMethod` (sync) for switching between already-loaded languages
- `setLanguageUsingActivity` — an `@UpdateMethod` (async) that calls an activity to fetch a
  greeting for a new language before switching
- `approve` — a `@SignalMethod` that allows the workflow to complete

The three `GreetingService` Nexus operations delegate to these workflow handlers via the Temporal
client inside each `OperationHandler.sync` implementation. The caller workflow sees only the Nexus
operations; the entity workflow is a private implementation detail.
