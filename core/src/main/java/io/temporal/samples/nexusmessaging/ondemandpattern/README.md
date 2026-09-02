## On-demand pattern

No Workflow is pre-started. The caller creates and controls Workflow instances through Nexus
operations. `NexusRemoteGreetingService` adds a `runFromRemote` operation that starts a new
`GreetingWorkflow`, and every other operation includes a `workflowId` so the handler knows which
instance to target.

The caller Workflow:
1. Attaches approval context for the first user via `attachApprovalContext`, before anything has
   started that user's Workflow
2. Starts two remote `GreetingWorkflow` instances via `runFromRemote` (backed by a Workflow started
   through `TemporalNexusClient.startWorkflow`)
3. Attaches approval context for the second user, whose Workflow now already exists
4. Queries each for supported languages
5. Changes the language on each (Arabic and Hindi)
6. Confirms the changes via queries
7. Approves both Workflows
8. Waits for each to complete and returns their results

### Running

This sample requires a Temporal dev server build that supports Workflow Update callbacks. Download the compatible
binary from the [Temporal CLI pre-release instructions](https://docs.temporal.io/standalone-nexus-operation#temporal-cli-support).

Start the Temporal dev server with the required namespaces pre-created and Workflow Update callbacks enabled:

```bash
./temporal server start-dev \
  --dynamic-config-value history.enableUpdateCallbacks=true \
  --dynamic-config-value history.enableCHASMSignalBacklinks=true \
  --dynamic-config-value history.enableSignalWithStartFromWorkflow=true \
  --namespace nexus-messaging-handler-namespace \
  --namespace nexus-messaging-caller-namespace
```

Create the Nexus endpoint:

```bash
./temporal operator nexus endpoint create \
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
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusmessaging.ondemandpattern.handler.HandlerWorker
```

In a second terminal, start the caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusmessaging.ondemandpattern.caller.CallerRemoteWorker
```

In a third terminal, run the following command to start the example:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusmessaging.ondemandpattern.caller.CallerRemoteStarter
```

Expected output:

```
Attached approval context before the workflow existed: UserId One
Started remote greeting workflow: UserId One
Started remote greeting workflow: UserId Two
Attached approval context to the running workflow: UserId Two
Supported languages for UserId One: [CHINESE, ENGLISH]
Supported languages for UserId Two: [CHINESE, ENGLISH]
UserId One changed language: ENGLISH -> ARABIC
UserId Two changed language: ENGLISH -> HINDI
Workflows approved
Workflow one result: مرحبا بالعالم
Workflow two result: नमस्ते दुनिया
```
