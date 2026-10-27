# Parallel agents

ADK's `ParallelAgent` runs distinct planner and reviewer agents using the scheduler supplied by
`TemporalAdk.runnerBuilder()`. Their model Activities are scheduled concurrently, and the Workflow
returns their answers in a stable order. Do not set an agent-specific scheduler or executor.

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
temporal workflow execute --type MultiAgentWorkflow --task-queue google-adk-samples --input '"Chicago"'
```

Offline mode returns `Bring a warm jacket.` and `Visit Chicago's lakefront.`. Live Gemini generates
both specialists' answers. The tests verify that both model Activities are scheduled before either
completes and that replay makes no further model calls.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.MultiAgentWorkflowTest
```
