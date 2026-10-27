# Single-turn agent

One ADK turn schedules a `googleadk.InvokeModel` Activity. The existing example and snippet IDs are
retained for documentation; [Agent Chat](../chat) is the entry point for conversations.

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
temporal workflow execute --type BasicWorkflow --task-queue google-adk-samples --input '"Hello"'
```

Offline mode returns `turns=1; latest: Hello`; live mode returns Gemini's response. The UI shows one
model Activity. Replaying its history reads the recorded response without another provider call.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.BasicWorkflowTest
```
