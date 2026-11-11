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

For synchronous operations, the SDK supplies `NexusSerializationContext` when decoding
inputs and encoding results. For asynchronous operations backed by `EchoWorkflow`, the
handler's SDK sends the context when starting the workflow, and the server stores it.
The workflow worker receives that context and uses it to encode the workflow's result
for the Nexus caller. Other workflow payloads keep their normal workflow context.

The sample's `AesGcmCodec.withContext` reads the endpoint directly from this context
and includes it in the encrypted result's metadata. No custom interceptor or workflow
header propagation is needed. The SDK-side codec chooses the key; the server carries
the context without access to encryption keys.

The hard-coded keys are only for this local example. For production encryption,
use a secure key store, as in the
[AWS Encryption SDK sample](../keymanagementencryption/awsencryptionsdk/README.md).

## Prerequisites

Use a Temporal Server that supports Nexus serialization-context propagation for
workflow-backed asynchronous operations. For local development, download
[Temporal CLI `v1.9.3-nexus-serialization-context`](https://github.com/temporalio/cli/releases/tag/v1.9.3-nexus-serialization-context)
and add its `temporal` binary to your `PATH`. Its dev server includes this support.

## Run locally

Start the dev server:

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
