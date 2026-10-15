# Nexus Secure Credential Propagation

This sample propagates an end user's JWT from a client, through a caller Workflow, to a Nexus Operation handler that
authorizes the operation on the user's behalf. The JWT never appears in plaintext in Workflow history, Temporal
headers, or Nexus headers.

The security claim is **context-bound, receiver-encrypted credential propagation with replay-safe idempotent
execution**. The scheme does not detect that a valid ciphertext was accepted before, so it is not statelessly
replay-proof, and it does not protect against compromised Workers.

> The demo stores keysets unencrypted on local disk and issues its own JWTs. Production deployments load keysets
> encrypted with a KMS key, give each process only the keys its role needs, and obtain tokens from a real identity
> provider.

## How it works

```
Client                     Caller Workflow Worker            Temporal / Nexus           Handler Worker
  |  start(args)             |                                   |                          |
  |  header = HPKE(wf.pub,   |                                   |                          |
  |    jwt+intent,           |                                   |                          |
  |    ns|type|wid|D(args))  |                                   |                          |
  |------------------------->| open with wf.priv, recompute      |                          |
  |                          | D(args) from received ciphertexts |                          |
  |                          |                                   |                          |
  |                          | Nexus input ciphertext C          |                          |
  |                          | header = HPKE(nexus.pub,          |                          |
  |                          |   jwt+intent,                     |                          |
  |                          |   endpoint|svc|op|D(C))           |                          |
  |                          |---------------------------------->|------------------------->|
  |                          |                                   |  route from dispatch,    |
  |                          |                                   |  D(C) from received C,   |
  |                          |                                   |  open with nexus.priv,   |
  |                          |                                   |  verify JWT, check       |
  |                          |                                   |  input against intent    |
```

`D(...)` is the **args digest**: SHA-256 over the transmitted argument ciphertexts, their positions, and their count.
Binding the credential to it means the credential only opens next to the exact arguments it was sent with. Nothing
hashes or canonicalizes the business input, so serialization does not need to be deterministic.

### Keys

| Keyset | Type | Starter | Caller Worker | Handler Worker |
|---|---|---|---|---|
| Payload | AEAD (symmetric) | yes | yes | yes |
| Workflow credential | HPKE | public | private + public | - |
| Nexus credential | HPKE | - | public | private |
| Operation token | MAC | - | - | yes |
| Demo identity provider | JWT ES256 | private | - | public |

The credential headers use hybrid public-key encryption to the receiving hop, so read access exists only where the
credential is consumed. Clients and caller Workers hold only public keys for the hops they send on, so a sender-side
compromise reveals no credential in history. Hybrid encryption does not authenticate the sender. That is fine here:
every header is bound to an args digest that cannot be forged without the payload key, and must contain a JWT that
the handler validates. Holding the payload key alone does not let an attacker repurpose a credential either: changing
any argument changes the digest.

Payloads stay symmetric because both ends of a payload hop must read them. The codec binds each payload to its
serialization context (Nexus endpoint/service/operation, or Workflow namespace/ID), and never falls back to weaker
associated data.

### Public keys are public

The HPKE public keys do not need to be kept secret. A public key lets its holder seal a credential header, but not
open one, and not move an existing header to another context or other arguments: that binding is authenticated by
HPKE and can only be produced from the plaintext. So the only header a public-key holder can create carries a JWT
they already possess, and they act as themselves. The handler validates that JWT and authorizes the principal against
the input no matter who sealed the header, and the Temporal access layer still decides who may start Workflows or
call the endpoint.

What a public key does not give the handler is **sender authentication**: it cannot tell a header created by a
legitimate caller Worker from one created by anyone else holding a valid token. Keeping the public key private adds
some defence in depth (for example against a leaked token being injected through Temporal by a party that is already
an allowed caller), but it is not the right control. To accept only headers created by legitimate caller Workers,
sign them:

- The caller Worker signs `frame(context, headerCiphertext)` with a Tink `PublicKeySign` key held only by caller
  Workers, and sends the signature alongside the header.
- The handler verifies it with the corresponding `PublicKeyVerify` key before opening the header.

The secret is then the signing key. Tink's `HybridEncrypt` uses HPKE's base mode and does not expose its authenticated
mode, so a separate signature is the practical way to add this. The sample does not implement it.

### Wire formats

```
Temporal header "end-user-credential":
  v1.start.<base64url HPKE(..., context = frame("workflow-credential-start", v1, namespace, workflowType, workflowId, D(args)))>
  v1.can.<base64url HPKE(..., context = frame("workflow-credential-can", v1, namespace, workflowType, workflowId, firstExecutionRunId))>

Nexus header "x-temporal-end-user-credential":
  v1.<base64url HPKE(..., context = frame("nexus-credential", v1, endpoint, service, operation, D(input)))>

plaintext = frame("credential-envelope", v1, compactJwt, intent)
```

`frame` is a length-prefixed encoding. The context is visible to anyone who can read history; only the plaintext is
secret.

The Java SDK builds the continue-as-new header before encoding the new run's arguments, so continue-as-new headers
are bound to `firstExecutionRunId` instead of an args digest. The server preserves that ID across continue-as-new,
retries, cron, and reset, and assigns a fresh one to every new start, so a copied continue-as-new header is rejected
outside its original chain.

### Codec handoff

The payload codec knows nothing about credentials. It reports the digest of every list it encodes or decodes to
`ArgsDigest` through a thread local:

- When sending, the interceptor arms a pending seal before calling into the SDK. The SDK encodes the arguments first,
  which completes the seal into the live header map, and only then serializes the headers.
- When receiving, the SDK decodes the arguments immediately before invoking the interceptor, which takes the digest.

This depends on the SDK encoding and decoding arguments synchronously on the interceptor's thread, and on the
credential interceptors being innermost when sending (`CredentialWorkflowClientInterceptor` registered **first** on
the client, `CredentialCallerWorkerInterceptor` registered **last** on the Worker). Every failure mode fails closed:
an unbound start is reported by the client and has no credential at the Worker, an unbound Nexus call fails the
Workflow task, and a missing or mismatched header is rejected by the receiver. The tests pin these orderings.

### Authorization and steering

The handler gets the route from the actual Nexus request, never from custom headers. It decrypts the credential,
validates the JWT (signature, algorithm, issuer, audience, type, `exp`, `nbf`), then:

- authorizes the principal against the input: the token subject and tenant are authoritative, and the input's
  `userId` and `tenantId` are only assertions to check;
- requires the input to fall within the **intent** the client sealed next to the JWT (exact payment ID and
  beneficiary, maximum amount).

The args digest proves only that the caller Worker produced the input. Signals, updates, and activity results can
steer a caller Workflow into choosing different inputs. The intent bounds that: the Workflow carries it unchanged and
cannot alter it.

The intent protects a user from having their own Workflow steered; it does not constrain a user acting with their
own token. Anyone holding a valid token and a public key can seal any intent they like, so the intent is never a
substitute for server-side authorization. `TransferIntentPolicy` checks scope, subject, and tenant independently of
the intent for this reason.

Authorizing the steering messages themselves needs a separate mechanism, such as:

- **identity-provider-issued intent**, for example RFC 9396 `authorization_details` or transaction tokens, which also
  survive a caller Worker compromise (recommended for production);
- **authenticated steering messages** that carry the sender's own credential to the handler;
- **Temporal-layer restrictions** on who may signal or update these Workflows;
- **provenance discipline**, deriving Nexus input only from start arguments (a review rule, not a control).

Failures are non-retryable and use generic messages: malformed or unbindable input is `BAD_REQUEST`, a missing or
invalid credential is `UNAUTHENTICATED`, and a valid identity without permission is `UNAUTHORIZED`.

### Replay and idempotency

| Case | Outcome |
|---|---|
| Workflow replay | Interceptors decrypt again; regenerated ciphertexts are not compared by Temporal. |
| Nexus retry | Same input and request ID; resolves to the same backing Workflow. |
| Exact replay of a Nexus input/header pair from another caller | Attaches to the same backing Workflow while it runs; refused once it has closed. No second effect. |
| Exact replay of a start into a new run of the same ID | Re-runs the victim's own arguments and intent; its Nexus calls are exact replays. Bounded by JWT expiry. |

The backing Workflow ID is derived from the verified principal and the payment ID, never from the Nexus request ID.
Including the principal stops another user with identical input from attaching to someone else's transfer. Workflow
ID reuse protection is bounded by retention; enforce uniqueness beyond that in a durable store.

### Cancellation and the access-layer boundary

Operation tokens are signed with a handler-only key, so no caller can forge a token naming another user's backing
Workflow. Any holder of an issued token can cancel, including an exact replayer. A replayer presents byte-identical
input, headers, and token, so nothing at the handler can tell them apart from the legitimate caller. Anyone who can
both read caller history and reach the endpoint can replay starts (harmless) and cancels (effective). Restrict that at
the access layer: allowed callers on the Nexus endpoint, and client authentication for direct Nexus calls.

### Scope

- Only start paths (start, signal-with-start, update-with-start) carry a credential; a Workflow acts for one principal.
- The credential survives continue-as-new, including to a different Workflow type and through retries.
- Child Workflows never receive it.
- Protected Workflows and operations must take at least one argument.
- The Temporal Service is trusted for routing integrity (endpoint dispatch, request IDs, run lineage), not for
  confidentiality.
- Handler-side endpoint context requires Temporal Server 1.30 or newer; protected operations fail closed without it.
- Use short-lived, audience- and scope-limited tokens obtained by a token exchange at the client edge. Never forward
  a general login token or a refresh token through a long-running Workflow. Only the handler validates the token, so
  narrowing it adds no work at intermediate hops.

## Running the sample

Set up the namespaces and the local development server as in the main [Nexus sample](../nexus/README.MD), then create
the endpoint for this sample:

```
temporal operator nexus endpoint create \
  --name nexus-secure-credential-endpoint \
  --target-namespace my-target-namespace \
  --target-task-queue nexus-secure-credential-handler
```

In production, also restrict the endpoint's allowed callers.

Generate the demo keysets (written to `build/nexus-secure-credential-keys`; override with
`-Dnexus.secure.credential.keyDir=...`):

```
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexussecurecredentialpropagation.common.GenerateDemoKeys
```

Then, in separate terminal windows:

```
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexussecurecredentialpropagation.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

```
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexussecurecredentialpropagation.caller.CallerWorker \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

```
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexussecurecredentialpropagation.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

The starter logs `Transfer payment-1001 SETTLED for alice`. Inspect the caller Workflow's history
(`temporal workflow show -w transfer-payment-1001 -n my-caller-namespace --output json`): the start header and the
Nexus header contain only ciphertext.

### Verify the binding

The args digest is not a field in the header; it is authenticated associated data that the receiver recomputes. To
check the binding in a real history, run the verifier against the caller namespace:

```
./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexussecurecredentialpropagation.tools.VerifyCredentialBinding \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

For each `WorkflowExecutionStarted` and `NexusOperationScheduled` event that carries a credential header, it
recomputes the args digest from the event's input ciphertexts (or uses `firstExecutionRunId` for continue-as-new
headers), opens the header with that context, confirms that the same context with one bit changed is rejected, and
confirms the digest does not appear in the header bytes. It exits non-zero if any check fails. Set
`-Dnexus.secure.credential.workflowId=...` (for example via `JAVA_TOOL_OPTIONS`) to check another Workflow.

The verifier needs the receivers' private keys, so treat it as an operator tool. It prints only each credential's
subject and issuer, never the token.
