## Entity pattern

The handler worker starts a single `GreetingWorkflow` with a fixed workflow ID.
`NexusGreetingServiceImpl` holds that ID and routes every Nexus operation to it. The caller's
inputs contain only business data — no workflow IDs.

The caller workflow:
1. Queries for supported languages (`getLanguages` — backed by a `@QueryMethod`)
2. Changes the language to Arabic (`setLanguage` — backed by an `@UpdateMethod` that calls an activity)
3. Confirms the change via a second query (`getLanguage`)
4. Approves the workflow (`approve` — backed by a `@SignalMethod`)

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

In one terminal, start the handler worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.callerpattern.handler.HandlerWorker
```

In a second terminal, start the caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.callerpattern.caller.CallerWorker
```

In a third terminal, start the caller workflow:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.callerpattern.caller.CallerStarter
```

Expected output:

```
Supported languages: [CHINESE, ENGLISH]
Language changed: ENGLISH -> ARABIC
Workflow approved
```
