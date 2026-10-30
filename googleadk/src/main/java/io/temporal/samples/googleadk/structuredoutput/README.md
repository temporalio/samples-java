# Structured output

ADK's `outputSchema` supplies a JSON schema to the model. The Workflow parses the reply into a
`TravelPlan` and validates required values before returning the typed result. Malformed JSON,
fractional days, trailing JSON, and invalid fields fail with the non-retryable `InvalidTravelPlan`
failure instead of repeatedly failing a Workflow Task.

Run commands from the repository root. Start `temporal server start-dev` in one terminal.
In another, choose the credential-free offline Worker or a live Gemini Worker:

```bash
ADK_MODEL_MODE=offline ./gradlew :googleadk:worker
```

```bash
GOOGLE_API_KEY=your_key ADK_MODEL_MODE=live ./gradlew :googleadk:worker
```

Stop the Worker before switching modes. See the [suite README](../../../../../../../../README.md)
for prerequisites and integration limits.

In a third terminal:

```bash
temporal workflow execute --type StructuredOutputWorkflow --task-queue google-adk-samples --input '"Plan two days in Chicago."'
```

Offline mode returns `{"city":"Chicago","days":2,"activities":["Walk the lakefront","Visit a museum"]}`.
Live Gemini supplies its own plan with the same shape. History contains one `googleadk.InvokeModel`
Activity. Tests verify the schema on the worker-side request, typed results, invalid replies, and replay.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.StructuredOutputWorkflowTest
```
