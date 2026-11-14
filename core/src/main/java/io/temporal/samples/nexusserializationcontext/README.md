# Nexus serialization context

This sample uses `NexusSerializationContext` to select an encryption key for each Nexus
endpoint. The caller invokes synchronous and workflow-backed asynchronous operations
through four endpoints. `CompressedEncryptedHandlerWorker` compresses and then
encrypts Nexus payloads. `EncryptedHandlerWorker` encrypts without
compression. Each handler selects between its own key 1 and key 2 using
the endpoint in the Nexus context.
The caller and each handler run in separate namespaces.

| Handler | Endpoint | Encryption key | Nexus payload encoding |
|---|---|---|---|
| `CompressedEncryptedHandlerWorker` | `nexus-serialization-compressed-encrypted-1` | `COMPRESSED_ENCRYPTED_KEY_1` | zlib, then AES-GCM |
| `CompressedEncryptedHandlerWorker` | `nexus-serialization-compressed-encrypted-2` | `COMPRESSED_ENCRYPTED_KEY_2` | zlib, then AES-GCM |
| `EncryptedHandlerWorker` | `nexus-serialization-encrypted-1` | `ENCRYPTED_KEY_1` | AES-GCM |
| `EncryptedHandlerWorker` | `nexus-serialization-encrypted-2` | `ENCRYPTED_KEY_2` | AES-GCM |

These four endpoint keys are distinct 128-bit AES keys. Compression runs before encryption;
decoding decrypts before decompressing. AES-GCM authenticates the ciphertext.
The AES-GCM and zlib codecs are implemented in this sample; Nexus supplies the endpoint
context used to select them.

Each worker configures `NexusCodec` with an endpoint-to-codec map and a default codec
for ordinary workflow payloads. The caller uses a separate `CALLER_WORKFLOW_KEY` for
its own input and final result, keeping those payloads independent of the keys shared
with Nexus handlers. Each handler reuses its key 1 for backing workflow inputs.

| Ordinary workflow payload | Encryption key |
|---|---|
| Caller workflow input and final result | `CALLER_WORKFLOW_KEY` |
| Compressed/encrypted handler workflow inputs | `COMPRESSED_ENCRYPTED_KEY_1` |
| Encrypted handler workflow inputs | `ENCRYPTED_KEY_1` |

These workflow payloads use AES-GCM without compression or endpoint metadata.
An async Nexus result uses the key and compression chain selected by its endpoint,
even when that differs from the backing workflow's input encryption.

For synchronous operations, the SDK supplies Nexus context when decoding inputs and
encoding results. When an asynchronous handler starts `EchoWorkflow`, the SDK includes
the Nexus endpoint, service, and operation in the workflow-start request. The server
stores these fields in the `WorkflowExecutionStarted` history event. The worker SDK
restores the Nexus context from that event when encoding the workflow's result, so
`NexusCodec` can select the endpoint's codec and key from the worker's local configuration.
The workflow's input still uses ordinary workflow context; this sample's default codec
encrypts it with that handler's key 1.

The caller starts all eight operations before waiting for their results. Each operation
keeps its own endpoint context, even when operations for different keys are in flight.
No custom interceptor or workflow-header propagation is required.

The outer encrypted payload includes `encoding`, `encryption-key-id`, and, for Nexus
payloads, `nexus-endpoint-name` metadata. For example:

```text
encoding: binary/nexus-aes-gcm
encryption-key-id: compressed-encrypted-1
nexus-endpoint-name: nexus-serialization-compressed-encrypted-1
```

A Codec Server can use that metadata to select the key and decompression chain without
SDK context. Ordinary workflow payloads have a key ID but no endpoint metadata or compression.

The hard-coded keys are only for this local example. For production encryption,
use a secure key store, as in the
[AWS Encryption SDK sample](../keymanagementencryption/awsencryptionsdk/README.md).

## Prerequisites

Use a Temporal Server that supports Nexus serialization-context propagation for
workflow-backed asynchronous operations. For local development, download
[Temporal CLI `v1.9.3-nexus-serialization-context`](https://github.com/temporalio/cli/releases/tag/v1.9.3-nexus-serialization-context).
Keep this prerelease binary separate from your stable CLI. The commands below use
`./temporal` to run the downloaded binary from the repository root.

## Run locally

The caller and each handler use separate namespaces. Each worker polls a task queue
with the same name as its namespace. Both endpoints for a handler target that handler's
namespace and task queue.

| Worker | Namespace and task queue |
|---|---|
| Caller | `nexus-serialization-caller` |
| Compressed/encrypted handler | `nexus-serialization-compressed-encrypted-handler` |
| Encrypted handler | `nexus-serialization-encrypted-handler` |

Start the dev server:

```bash
./temporal server start-dev \
  --namespace nexus-serialization-caller \
  --namespace nexus-serialization-compressed-encrypted-handler \
  --namespace nexus-serialization-encrypted-handler
```

Create two endpoints for each handler worker:

```bash
./temporal operator nexus endpoint create \
  --name nexus-serialization-compressed-encrypted-1 \
  --target-namespace nexus-serialization-compressed-encrypted-handler \
  --target-task-queue nexus-serialization-compressed-encrypted-handler
./temporal operator nexus endpoint create \
  --name nexus-serialization-compressed-encrypted-2 \
  --target-namespace nexus-serialization-compressed-encrypted-handler \
  --target-task-queue nexus-serialization-compressed-encrypted-handler
./temporal operator nexus endpoint create \
  --name nexus-serialization-encrypted-1 \
  --target-namespace nexus-serialization-encrypted-handler \
  --target-task-queue nexus-serialization-encrypted-handler
./temporal operator nexus endpoint create \
  --name nexus-serialization-encrypted-2 \
  --target-namespace nexus-serialization-encrypted-handler \
  --target-task-queue nexus-serialization-encrypted-handler
```

Run each of the following in its own terminal from the repository root:

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.handler.CompressedEncryptedHandlerWorker \
  --args="-namespace nexus-serialization-compressed-encrypted-handler"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.handler.EncryptedHandlerWorker \
  --args="-namespace nexus-serialization-encrypted-handler"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.caller.CallerWorker \
  --args="-namespace nexus-serialization-caller"
```

```bash
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusserializationcontext.caller.CallerStarter \
  --args="-namespace nexus-serialization-caller"
```

The starter prints a sync result and an async result for each endpoint. All eight
results should be `Hello from Nexus`.

## Test

Run the codec tests:

```bash
./gradlew :core:test --tests 'io.temporal.samples.nexusserializationcontext.codec.NexusCodecTest'
```
