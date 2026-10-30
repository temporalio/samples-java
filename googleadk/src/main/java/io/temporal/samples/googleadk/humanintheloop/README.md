# Human approval

ADK's `requireConfirmation` pauses a shipping tool before its Activity runs. The Workflow exposes
pending calls through a Query and waits durably for a decision Signal. Shipping returns sample data.

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
temporal workflow start --workflow-id adk-approval --type ApprovalWorkflow --task-queue google-adk-samples --input '"Ship order demo-order, after confirmation."'
temporal workflow query --workflow-id adk-approval --name pendingConfirmations
temporal workflow signal --workflow-id adk-approval --name decide --input true
temporal workflow result --workflow-id adk-approval
```

Query again if the pending list is initially empty. Send `decide` only after a request appears;
use `--input false` on a fresh execution to reject instead. Offline results are `Order shipped.` or
`Order rejected.`. Live Gemini phrases the result. A rejection schedules no `ShipOrder` Activity.
A Signal decides the current pending batch; premature Signals are ignored and the first decision wins. If live
Gemini requests another confirmation, query and decide again.

To cancel while waiting, run `temporal workflow cancel --workflow-id adk-approval` instead of deciding.
The Workflow cancels without shipping. Tests cover approval, rejection, cancellation, and replay.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.ApprovalWorkflowTest
```
