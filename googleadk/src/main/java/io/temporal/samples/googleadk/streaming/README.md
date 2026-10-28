# Buffered ADK streaming through Workflow Streams

ADK Java supports SSE model responses. Version 0.1.0 of the Temporal plugin collects their chunks
inside one model Activity. This scenario publishes those partial and final events through the
experimental `temporal-workflowstreams` module after the Activity completes. Subscribers receive
ordered chunks at that point; live token forwarding while the provider is running requires a plugin
change. Bidirectional live connections are unsupported through `TemporalModel`.

From the repository root, start `temporal server start-dev` in one terminal. In another, choose a
credential-free Worker or a live Gemini Worker:

```bash
ADK_MODEL_MODE=offline ./gradlew :googleadk:worker
```

```bash
GOOGLE_API_KEY=your_key ADK_MODEL_MODE=live ./gradlew :googleadk:worker
```

Stop the Worker before switching modes. Start a Workflow and stream subscriber in a third terminal:

```bash
./gradlew :googleadk:starter --args='streaming'
```

Offline mode prints two partial chunks (`Hello ` and `from ADK`), a final `Hello from ADK`, and a
`complete` event. The subscriber acknowledges completion so the Workflow can return. With no
subscriber, the Workflow completes after a ten-second acknowledgement deadline:

```bash
temporal workflow execute --type StreamingWorkflow --task-queue google-adk-samples --input '"Say hello."'
```

Subscribe when starting the Workflow; the stream serves buffered items while the Workflow is open.
The acknowledgement deadline bounds the wait and does not provide a grace period for arbitrary late
subscribers after completion. Model output is recorded by one `googleadk.InvokeModel` Activity and
is not requested again during replay.

Test ordered partial/final delivery, subscriber completion, absence of a subscriber, and replay:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.StreamingWorkflowTest
```
