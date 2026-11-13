# Agent Chat

Send each user message as a Workflow Update and receive that turn's reply directly. Concurrent turns
run one at a time on the same ADK session. After three completed turns, the Workflow waits for accepted
Updates to finish, exports the session, and Continues-As-New with the latest reply.

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
temporal workflow start --workflow-id adk-chat --type ChatWorkflow --task-queue google-adk-samples --input '{"session":null,"lastReply":""}'
temporal workflow update execute --workflow-id adk-chat --name message --input '"My name is Ada."'
temporal workflow update execute --workflow-id adk-chat --name message --input '"Plan a trip to Chicago."'
temporal workflow update execute --workflow-id adk-chat --name message --input '"What should I pack?"'
```

Each Update waits for its own reply. In offline mode, the third response starts with `turns=3;`.
Continue the conversation across the Run transition, then finish:

```bash
temporal workflow update execute --workflow-id adk-chat --name message --input '"What is my name?"'
temporal workflow signal --workflow-id adk-chat --name finish
temporal workflow result --workflow-id adk-chat
```

The fourth offline reply is `turns=4; latest: What is my name?`. Live Gemini also sees the imported
conversation and can recall Ada's name. `finish` rejects new messages and lets accepted Updates finish
before returning the last reply. Blank messages are rejected before invoking the model.

The optional `lastReply` Query inspects the latest completed turn; Updates return their replies
without polling. The tests check concurrent Updates at a Run transition, draining accepted Updates
on finish, and replay of both histories. A session snapshot still grows with the conversation, so
bound or compact real conversations before they reach payload limits.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.ChatWorkflowTest
```
