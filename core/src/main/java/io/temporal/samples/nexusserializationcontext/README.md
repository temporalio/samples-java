# Nexus serialization context

This sample calls the same synchronous Nexus operation through two endpoints that
share a handler task queue. `NexusCodec` uses `NexusSerializationContext` to select
the `PayloadCodec` registered for each endpoint:

- One endpoint compresses with zlib, then encrypts with AES-GCM using Key A.
- The other endpoint encrypts with AES-GCM using Key B.

The caller schedules both operations before waiting for their results, so each
result must be decoded using the context of its own endpoint. Non-Nexus payloads
pass through the codec unchanged.

The encrypted payload metadata includes `binary/nexus-aes-gcm` and a sample key ID
(`key-a` or `key-b`), so you can see which key the endpoint selected.

`NexusSerializationContext` works end to end for synchronous Nexus operations.
The final result of an asynchronous operation does not receive `NexusSerializationContext`.

The hard-coded keys are only for this local example. For production encryption,
use a secure key store, as in the
[AWS Encryption SDK sample](../keymanagementencryption/awsencryptionsdk/README.md).

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
  --name nexus-serialization-compressed-encrypted \
  --target-namespace nexus-serialization-handler \
  --target-task-queue nexus-serialization-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-encrypted \
  --target-namespace nexus-serialization-handler \
  --target-task-queue nexus-serialization-handler
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
Compressed and encrypted endpoint result: Hello from Nexus
Encrypted endpoint result: Hello from Nexus
```
