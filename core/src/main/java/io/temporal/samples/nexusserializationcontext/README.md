# Nexus serialization context

This sample calls two synchronous and two asynchronous Nexus operations through
four endpoints. The synchronous endpoints have separate handler workers. The two
asynchronous endpoints share a handler namespace, task queue, and worker. The caller's
`NexusCodec` uses `NexusSerializationContext` to select the `PayloadCodec`
registered for each endpoint. The sample uses these keys:

- Key A: compresses with zlib, then encrypts the first synchronous endpoint's payloads.
- Key B: encrypts the second synchronous endpoint's payloads.
- Key C: encrypts one asynchronous endpoint's payloads.
- Key D: compresses with zlib, then encrypts the other asynchronous endpoint's payloads.
- Key E: encrypts the caller workflow's input and final result.

The caller configures all four endpoint codecs and a separate codec for its own
workflow input and result. The synchronous handler workers each use a fixed key. The
shared asynchronous worker has both Keys C and D and selects a codec by endpoint.

The caller schedules all four operations before waiting for their results, so each
result must be decoded using the context of its own endpoint. The caller's
`NexusCodec` uses Key E for its own workflow payloads, which have no Nexus endpoint.

For the four Nexus endpoints, the outer encrypted payload stores the endpoint name in
`nexus-endpoint-name` metadata,
alongside `binary/nexus-aes-gcm` and a sample key ID (`key-a`, `key-b`, `key-c`, or `key-d`). A Codec
Server can use the endpoint name to select the matching key and decompression
chain without SDK context.

For the compressed endpoint, the outer payload's decoded metadata looks like:

```text
encoding: binary/nexus-aes-gcm
encryption-key-id: key-a
nexus-endpoint-name: nexus-serialization-compressed-encrypted
```

The asynchronous results have `encryption-key-id: key-c` or `key-d` and their respective
endpoint names in the outer payload metadata.
The caller workflow's input and final result use `encryption-key-id: key-e`; they do not
have endpoint metadata. The starter uses the same converter as the caller worker, so it
can decode the final result before printing it.

`NexusSerializationContext` works end to end for synchronous Nexus operations.
For an asynchronous operation, the handler's final result is serialized as a workflow
result and does not receive `NexusSerializationContext`. This sample's
`NexusEndpointInterceptor` captures the endpoint when the handler starts the backing
workflow. `NexusEndpointContextPropagator` saves it in the workflow headers and restores
it on the workflow thread. The shared worker's `PropagatedEndpointCodec` uses the endpoint
to choose Key C or the Key D compression and encryption chain when encoding the workflow
result. Its `AesGcmCodec` also includes the endpoint name in the encrypted result metadata.
The caller uses `NexusSerializationContext` to decode each asynchronous result.

This workaround covers asynchronous operations backed by workflows. If the propagated
endpoint is missing, the shared worker's codec rejects the payload instead of leaving
it unencrypted. The shared service implementation exposes both operations on every
worker, although the caller invokes `echoAsync` only through the two async endpoints.

The hard-coded keys are only for this local example. For production encryption,
use a secure key store, as in the
[AWS Encryption SDK sample](../keymanagementencryption/awsencryptionsdk/README.md).

Requires Java SDK 1.40.0 or later and Temporal Server 1.30.0 or later with Nexus enabled
so the handler can read the endpoint name.

## Run locally

Start a Temporal dev server:

```bash
temporal server start-dev
```

In another terminal, create the namespaces and endpoints:

```bash
temporal operator namespace create --namespace nexus-serialization-caller
temporal operator namespace create --namespace nexus-serialization-key-a-handler
temporal operator namespace create --namespace nexus-serialization-key-b-handler
temporal operator namespace create --namespace nexus-serialization-async-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-compressed-encrypted \
  --target-namespace nexus-serialization-key-a-handler \
  --target-task-queue nexus-serialization-key-a-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-encrypted \
  --target-namespace nexus-serialization-key-b-handler \
  --target-task-queue nexus-serialization-key-b-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-async-encrypted \
  --target-namespace nexus-serialization-async-handler \
  --target-task-queue nexus-serialization-async-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-async-compressed-encrypted \
  --target-namespace nexus-serialization-async-handler \
  --target-task-queue nexus-serialization-async-handler
```

Run each of the following in its own terminal from the repository root:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.handler.CompressedEncryptedHandlerWorker \
  --args="-namespace nexus-serialization-key-a-handler"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.handler.EncryptedHandlerWorker \
  --args="-namespace nexus-serialization-key-b-handler"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.handler.AsyncHandlerWorker \
  --args="-namespace nexus-serialization-async-handler"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.caller.CallerWorker \
  --args="-namespace nexus-serialization-caller"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.caller.CallerStarter \
  --args="-namespace nexus-serialization-caller"
```

The starter will print:

```text
Compressed and encrypted endpoint sync result: Hello from Nexus
Encrypted endpoint sync result: Hello from Nexus
Async encrypted endpoint result: Hello from Nexus
Async compressed and encrypted endpoint result: Hello from Nexus
```
