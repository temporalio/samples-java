# Nexus serialization context

This sample calls a synchronous and an asynchronous Nexus operation through each of
two endpoints. Each endpoint routes to its own handler worker, which registers both
`SyncEchoService` and `AsyncEchoService`. The caller's `NexusCodec` uses
`NexusSerializationContext` to select the `PayloadCodec` registered for each endpoint.
The sample uses these keys:

- Key A: compresses with zlib, then encrypts synchronous and asynchronous payloads for `nexus-serialization-compressed-encrypted`.
- Key B: encrypts synchronous and asynchronous payloads for `nexus-serialization-encrypted`.
- Key C: encrypts the caller workflow's input and final result.

The caller configures a codec for each endpoint and a separate codec for its own
workflow input and result. Each handler worker uses the fixed key for its endpoint.

The caller schedules all four operations before waiting for their results, so each
result must be decoded using the context of its own endpoint. The caller's
`NexusCodec` uses Key C for its own workflow payloads, which have no Nexus endpoint.

For both Nexus endpoints, the outer encrypted payload stores the endpoint name in
`nexus-endpoint-name` metadata alongside `binary/nexus-aes-gcm` and a sample key ID
(`key-a` or `key-b`). A Codec Server can use the endpoint name to select the matching
key and decompression chain without SDK context.

For the compressed endpoint, the outer payload's decoded metadata looks like:

```text
encoding: binary/nexus-aes-gcm
encryption-key-id: key-a
nexus-endpoint-name: nexus-serialization-compressed-encrypted
```

The asynchronous results have `encryption-key-id: key-a` or `key-b` and their respective
endpoint names in the outer payload metadata.
The caller workflow's input and final result use `encryption-key-id: key-c`; they do not
have endpoint metadata. The starter uses the same converter as the caller worker, so it
can decode the final result before printing it.

`NexusSerializationContext` works end to end for synchronous Nexus operations.
For an asynchronous operation, the handler's final result is serialized as a workflow
result and does not receive `NexusSerializationContext`. We plan to add
`NexusSerializationContext` support for asynchronous operation results in the Java SDK.
Until then, the sample's `NexusEndpointInterceptor` captures the endpoint when the
handler starts the backing `EchoWorkflow`. `NexusEndpointContextPropagator` saves it in
the workflow headers and restores it on the workflow thread. Each handler's codec uses
its fixed key to encrypt the workflow result and includes the propagated endpoint name
in the outer payload metadata. This propagation is needed for the endpoint metadata;
the handler already knows which key to use. When the result reaches the caller, the SDK
supplies `NexusSerializationContext` to decode it.

This endpoint propagation covers asynchronous operations backed by workflows. If the
endpoint is not propagated, the result remains encrypted with the handler's fixed key
but lacks the endpoint name in its metadata.

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
temporal operator nexus endpoint create \
  --name nexus-serialization-compressed-encrypted \
  --target-namespace nexus-serialization-key-a-handler \
  --target-task-queue nexus-serialization-key-a-handler
temporal operator nexus endpoint create \
  --name nexus-serialization-encrypted \
  --target-namespace nexus-serialization-key-b-handler \
  --target-task-queue nexus-serialization-key-b-handler
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
Compressed and encrypted endpoint async result: Hello from Nexus
Encrypted endpoint async result: Hello from Nexus
```
