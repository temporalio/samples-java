## Caller pattern

The handler worker starts a `GreetingWorkflow` for a User ID.
`NexusGreetingServiceImpl` derives the Workflow ID and routes every Nexus operation to it. 
The caller's input does not have that Workflow ID as the caller doesn't know it - but the caller sends in the User ID,
and `NexusGreetingServiceImpl` knows how to get the desired Workflow ID from that User ID (see the `getWorkflowId` call).

`HandlerWorker` is using the same `getWorkflowId` call to generate a Workflow ID from a User ID when it launches the Workflow.

The caller Workflow:
1. Queries for supported languages (`getLanguages` — backed by a `@QueryMethod`)
2. Changes the language to Arabic (`setLanguage` — backed by an `@UpdateMethod` that calls an activity)
3. Confirms the change via a second query (`getLanguage`)
4. Approves the Workflow (`approve` — backed by a `@SignalMethod`)

### Running

This sample requires a Temporal dev server build that supports Workflow Update callbacks. Download the compatible
binary from the [Temporal CLI pre-release instructions](https://docs.temporal.io/standalone-nexus-operation#temporal-cli-support).

Start the Temporal dev server with the required namespaces pre-created and Workflow Update callbacks enabled:

```bash
./temporal server start-dev \
  --dynamic-config-value history.enableUpdateCallbacks=true \
  --dynamic-config-value history.enableCHASMSignalBacklinks=true \
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
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusmessaging.callerpattern.handler.HandlerWorker
```

In a second terminal, start the caller worker:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusmessaging.callerpattern.caller.CallerWorker
```

In a third terminal, run the following command to start the example:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusmessaging.callerpattern.caller.CallerStarter
```

Expected output:

```
Supported languages: [CHINESE, ENGLISH]
Language changed: ENGLISH -> ARABIC
Workflow approved
```
