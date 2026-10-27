# Agent Chat

Hold a multi-turn ADK conversation using Signals and a Query. After three turns, export the session
and Continue-As-New with queued messages and the latest reply. `finish` drains the queue.

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
temporal workflow start --workflow-id adk-chat --type ChatWorkflow --task-queue google-adk-samples --input '{"session":null,"messages":[],"finished":false,"lastReply":""}'
temporal workflow signal --workflow-id adk-chat --name message --input '"My name is Ada."'
temporal workflow query --workflow-id adk-chat --name lastReply
temporal workflow signal --workflow-id adk-chat --name message --input '"Plan a trip to Chicago."'
temporal workflow signal --workflow-id adk-chat --name message --input '"What should I pack?"'
temporal workflow query --workflow-id adk-chat --name lastReply
```

Repeat the Query until the third reply appears. In offline mode it starts with `turns=3;`. Then
continue the conversation in the next Run:

```bash
temporal workflow signal --workflow-id adk-chat --name message --input '"What is my name?"'
temporal workflow signal --workflow-id adk-chat --name finish
temporal workflow result --workflow-id adk-chat
```

Queries may initially return an empty or previous reply while the model Activity runs. In offline
mode replies show the number of user turns and latest input. Wait for the third reply before sending
the fourth message to observe Continue-As-New; it returns `turns=4; latest: What is my name?`.
Live Gemini also sees the imported conversation and can recall Ada's name. The tests check queued
messages across a Run transition and replay both histories. A session snapshot still grows with the
conversation, so bound or compact real conversations before they reach payload limits.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.ChatWorkflowTest
```
