# Long-running tool completion

An Activity-backed long-running ADK tool submits a report and returns a pending status. The
Workflow waits for a correlated callback Signal, then uses `Hitl.functionResponse` to resume the
original call. No polling Activity or open model connection is needed while waiting.

The Activity simulates submission; it does not run a report service. A real service would save the
Workflow/call correlation and send the same Signal when its work finishes.

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
temporal workflow start --workflow-id adk-report --type LongRunningWorkflow --task-queue google-adk-samples --input '"Prepare a sales report."'
temporal workflow query --workflow-id adk-report --name pendingCall
temporal workflow signal --workflow-id adk-report --name complete --input '"CALL_ID"' --input '"Revenue grew 10%."'
temporal workflow result --workflow-id adk-report
```

Query until `pendingCall` is nonempty, then replace `CALL_ID` with that value. While waiting, wrong IDs and premature
callbacks are ignored, and the first matching result wins. Offline mode returns `Report complete: Revenue grew 10%.`;
live Gemini summarizes the result. History contains one `StartReport` Activity, with model calls
before submission, after the pending status, and after the final callback.

To cancel the durable wait, use `temporal workflow cancel --workflow-id adk-report`. This does not
cancel a job already submitted to a real backend; that needs a separate cancellation Activity.
Tests cover wrong-call correlation, cancellation, and replay without another submission.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.LongRunningWorkflowTest
```
