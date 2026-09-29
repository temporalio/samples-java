# Nexus serialization context

This sample calls the same synchronous Nexus operation through two endpoints. A
`PayloadCodec` uses `NexusSerializationContext` to choose HMAC for one endpoint and
zlib compression for the other. The caller schedules both operations before waiting
for their results, so each result must be decoded using the context of its own
endpoint. Non-Nexus payloads pass through the codec unchanged.

`NexusSerializationContext` works end to end for synchronous Nexus operations.
The final result of an asynchronous operation does not receive `NexusSerializationContext`.

HMAC authenticates payloads but does not encrypt them. The hard-coded key is only
for this local example. For production encryption, use a secure key store and a
codec such as the [AWS Encryption SDK sample](../keymanagementencryption/awsencryptionsdk/README.md).

Requires Java SDK 1.40.0 or later and a Temporal server with Nexus enabled.

## Run locally

Start a Temporal dev server:

```bash
temporal server start-dev
```

In another terminal, create the namespaces and endpoints:

```bash
temporal operator namespace create --namespace nexus-serialization-handler
temporal operator namespace create --namespace nexus-serialization-caller
temporal operator nexus endpoint create \
  --name nexus-serialization-hmac \
  --target-namespace nexus-serialization-handler \
  --target-task-queue nexus-serialization-hmac-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-zlib \
  --target-namespace nexus-serialization-handler \
  --target-task-queue nexus-serialization-zlib-handler
```

Run each of the following in its own terminal from the repository root:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.handler.HandlerWorker \
  --args="-namespace nexus-serialization-handler"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.caller.CallerWorker \
  --args="-namespace nexus-serialization-caller"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.caller.CallerStarter \
  --args="-namespace nexus-serialization-caller"
```

The starter prints:

```text
HMAC endpoint result: Hello from Nexus
zlib endpoint result: Hello from Nexus
```
