## On-demand pattern

No workflow is pre-started. The caller creates and controls workflow instances through Nexus
operations. `NexusRemoteGreetingService` adds a `runFromRemote` operation that starts a new
`GreetingWorkflow`, and every other operation includes a `workflowId` so the handler knows which
instance to target.

The caller workflow:
1. Starts two remote `GreetingWorkflow` instances via `runFromRemote` (backed by `WorkflowRunOperation`)
2. Queries each for supported languages
3. Changes the language on each (Arabic and Hindi)
4. Confirms the changes via queries
5. Approves both workflows
6. Waits for each to complete and returns their results

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

This sample loads connection settings from `ClientConfigProfile`. The
`nexus-messaging-handler` and `nexus-messaging-caller` profiles are defined in
`core/src/main/resources/config.toml`. You can override settings with environment
variables or by editing the TOML file (see the `envconfig` sample for details).

In one terminal, start the handler worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.ondemandpattern.handler.HandlerWorker
```

In a second terminal, start the caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.ondemandpattern.caller.CallerRemoteWorker
```

In a third terminal, start the caller workflow:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexus_messaging.ondemandpattern.caller.CallerRemoteStarter
```

Expected output:

```
started remote greeting workflow: UserId One
started remote greeting workflow: UserId Two
Supported languages for UserId One: [CHINESE, ENGLISH]
Supported languages for UserId Two: [CHINESE, ENGLISH]
UserId One changed language: ENGLISH -> ARABIC
UserId Two changed language: ENGLISH -> HINDI
Workflows approved
Workflow one result: مرحبا بالعالم
Workflow two result: नमस्ते दुनिया
```
