# Google ADK on Temporal

Run native [Google ADK Java](https://github.com/google/adk-java) agents in Temporal Workflows using
the standalone [`io.temporal:google-adk:0.1.0`](https://central.sonatype.com/artifact/io.temporal/google-adk)
integration. This integration is **Public Preview**, not Generally Available. APIs and configuration
may change before General Availability.

Model calls and MCP operations run as Temporal Activities. Agent orchestration, ordinary function
tools, and session state stay in the Workflow. Use `TemporalAdk.runnerBuilder()` to keep ADK's time,
identifiers, and parallel execution deterministic, and `TemporalAdk.run()` to await an agent turn.

## Requirements

- Java 17 or later.
- `io.temporal:google-adk:0.1.0`, which brings Google ADK Java 1.12.0.
- Temporal Java SDK and testing library 1.40.0 or later. The module declares a strict `[1.40.0,2.0)`
  version range, preferring 1.40.0, independently of the other samples' SDK version.
- For a live run, a local Temporal Service (`temporal server start-dev`) and `GOOGLE_API_KEY` in the
  Worker's environment. The Google GenAI client also supports Vertex AI's environment configuration.

The plugin's version is independent of the SDK version. The plugin does not supply `temporal-sdk`;
the application adds it explicitly. Dependencies come from Maven Central.

## Run a sample

Start a local Temporal Service:

```bash
temporal server start-dev
```

In another terminal, start the shared Worker from the repository root:

```bash
export GOOGLE_API_KEY=your_key
./gradlew :googleadk:worker
```

Then run one of these commands in a third terminal:

```bash
./gradlew :googleadk:starter --args='basic'
./gradlew :googleadk:starter --args='tools'
./gradlew :googleadk:starter --args='multiagent'
./gradlew :googleadk:starter --args='mcp'
```

The starter prints the agent's answer. All samples share the `google-adk-samples` Task Queue and
start a new Workflow ID for each invocation. Inspect their Activities at `http://localhost:8233`.

| Scenario | Source | What it shows |
| --- | --- | --- |
| Basic | [`basic/`](src/main/java/io/temporal/samples/googleadk/basic) | A native `LlmAgent` with `TemporalModel`; one model call becomes `googleadk.InvokeModel`. |
| Activity tools | [`tools/`](src/main/java/io/temporal/samples/googleadk/tools) | `ActivityTools.create` exposes a weather Activity to the agent. The Activity returns sample weather data; replace its body with provider I/O. |
| Multiple agents | [`multiagent/`](src/main/java/io/temporal/samples/googleadk/multiagent) | A `ParallelAgent` schedules two model Activities concurrently on Workflow threads. |
| MCP | [`mcp/`](src/main/java/io/temporal/samples/googleadk/mcp) | A self-contained Java stdio echo server, started on first use by the Worker. Listing and calling tools become Activities. No Node or separately running MCP service is required. |
| Human approval | [`humanintheloop/`](src/main/java/io/temporal/samples/googleadk/humanintheloop) | An Activity tool waits for a human decision delivered by Signal. The shipping Activity returns sample data and makes no fulfillment request. |
| Conversation | [`chat/`](src/main/java/io/temporal/samples/googleadk/chat) | Signals queue messages, a Query returns the latest reply, and a session snapshot preserves the conversation through Continue-As-New. |

[`SampleWorker`](src/main/java/io/temporal/samples/googleadk/SampleWorker.java) registers the
`GoogleAdkPlugin` on the Temporal Client, so its Workers inherit the model/MCP Activities and payload
converter. Credentials stay on the Worker. Native GenAI retries are disabled with one HTTP attempt;
Temporal Activity retries handle model failures. Register the plugin on external clients too, as
[`SampleStarter`](src/main/java/io/temporal/samples/googleadk/SampleStarter.java) does.

## Approve or reject a tool call

Start a fulfillment Workflow:

```bash
./gradlew :googleadk:starter --args='approval'
```

Use the printed Workflow ID in place of `WORKFLOW_ID`. Query the pending tool name and arguments,
then approve or reject the calls from that turn:

```bash
./gradlew :googleadk:starter --args='pending WORKFLOW_ID'
./gradlew :googleadk:starter --args='decide WORKFLOW_ID true'
# Or reject instead:
./gradlew :googleadk:starter --args='decide WORKFLOW_ID false'
./gradlew :googleadk:starter --args='result WORKFLOW_ID'
```

`pending` can initially be empty while the model runs; query again once the agent asks for approval.
The decision command sends a Signal; `result` waits for the final answer. The Workflow uses `Workflow.await` while paused,
then supplies `Hitl.confirmationResponse` to the next ADK turn. A rejected call never schedules the
shipping Activity. If the model asks for another confirmation, query and decide again.

## Continue a conversation as new

```bash
./gradlew :googleadk:starter --args='chat'
./gradlew :googleadk:starter --args='message WORKFLOW_ID My name is Ada.'
./gradlew :googleadk:starter --args='reply WORKFLOW_ID'
./gradlew :googleadk:starter --args='message WORKFLOW_ID Suggest a trip to Chicago.'
./gradlew :googleadk:starter --args='message WORKFLOW_ID What should I pack?'
./gradlew :googleadk:starter --args='message WORKFLOW_ID What is my name?'
./gradlew :googleadk:starter --args='reply WORKFLOW_ID'
./gradlew :googleadk:starter --args='finish WORKFLOW_ID'
```

`message` queues a turn without waiting for the model. Query again after the model Activity completes
to see its new reply. After three turns, the Workflow exports its ADK session, continues as new, and
imports the session in the next Run. Queued messages and the latest reply travel with the snapshot.
`finish` drains pending messages and returns the final answer. Continue-As-New limits each Run's
history; the snapshot still contains the conversation and must fit within Temporal's payload limits.

## Test without provider credentials

```bash
./gradlew :googleadk:test :googleadk:spotlessCheck
```

The tests use `ScriptedModel` from the published integration and `TestWorkflowEnvironment`; they
need no API keys or external Temporal Service. The MCP test starts the real local Java echo server.
Tests verify Activity retries and results, concurrent model scheduling, approval and rejection,
session snapshots and queued messages across Continue-As-New, and replay on a fresh Worker without
repeating model or tool calls.

## Limitations

- Plain ADK function tools run in the Workflow and must be deterministic. Put network, file, and
  other I/O in Activities or MCP tools.
- Build Runners with `TemporalAdk.runnerBuilder()`. Do not override agent-level schedulers or
  executors, or use RxJava blocking APIs inside a Workflow.
- Streaming chunks arrive after the model Activity completes. Token streaming out of a Workflow
  and live bidirectional mode are unsupported.
- Session snapshots preserve session-scoped state and events. They do not carry app-, user-, or
  temporary-scoped state.
- ADK OpenTelemetry spans are emitted again during replay when a tracing SDK is installed.

See the [integration README](https://github.com/temporalio/ai-integrations/tree/main/java/google-adk)
for the full API and limitations.
