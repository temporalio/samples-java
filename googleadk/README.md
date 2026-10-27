# Google ADK on Temporal

Run native [Google ADK Java](https://github.com/google/adk-java) agents durably with
[`io.temporal:google-adk:0.1.0`](https://central.sonatype.com/artifact/io.temporal/google-adk).
The integration is **Public Preview**; APIs and configuration may change before General Availability.

Model calls and MCP operations run as Temporal Activities. Agent orchestration, deterministic function
tools, and session state stay in the Workflow. Build Runners with `TemporalAdk.runnerBuilder()` and
await agent turns with `TemporalAdk.run()` so ADK uses Workflow time, identifiers, and threads.

## Requirements

- Java 17 or later.
- A local Temporal Service: `temporal server start-dev`.
- For live Gemini runs, `GOOGLE_API_KEY` in the Worker's environment, or Vertex AI configuration.

Dependencies come from Maven Central. This module supplies Temporal SDK/testing in `[1.40.0,2.0)`,
preferring 1.40.0, independently of other samples. The integration brings ADK Java 1.12.0.

## Run offline or with Gemini

Run every command from the repository root. Start the Service in one terminal:

```bash
temporal server start-dev
```

Start the Worker in a second terminal, choosing one model mode:

```bash
ADK_MODEL_MODE=offline ./gradlew :googleadk:worker
```

```bash
GOOGLE_API_KEY=your_key ADK_MODEL_MODE=live ./gradlew :googleadk:worker
```

`live` is the default. Offline mode uses a deliberately small, stateless scripted `BaseLlm` for these
scenarios, including fixed Chicago weather and travel examples. It makes no provider requests and
needs no key. Live mode constructs Gemini only on the Worker and disables GenAI's HTTP retries so
Temporal Activities own retries. Stop the Worker before switching modes.

Both modes run the same Worker registration, plugin, Workflows, Activities, messages, and result
path. The MCP scenario launches the real Java stdio server in both modes. Offline mode still needs
Temporal running, and a first Gradle build may download dependencies.

Use a third terminal for the scenario commands below. Every Worker polls `google-adk-samples`.
The Temporal UI at <http://localhost:8233> shows model and tool Activities. Use a new Workflow ID
when repeating a command that names an ID.

## Coverage

| Category | Scenario | Durable behavior |
| --- | --- | --- |
| Agent Chat | [chat](src/main/java/io/temporal/samples/googleadk/chat) | Signals queue turns; Queries expose replies; a session snapshot and pending messages survive Continue-As-New. |
| Tools | [tools](src/main/java/io/temporal/samples/googleadk/tools) | A nondeterministic weather observation runs in an Activity; a pure temperature conversion runs in the Workflow. |
| MCP | [mcp](src/main/java/io/temporal/samples/googleadk/mcp) | List and call a bundled stateless Java stdio server through Activities. |
| Streaming | Unsupported | Version 0.1.0 buffers ADK chunks until its model Activity completes; it has no Workflow Streams publisher or live bidirectional connection. |
| Human in the Loop | [humanintheloop](src/main/java/io/temporal/samples/googleadk/humanintheloop) | A Signal approves or rejects ADK's pending tool confirmation; cancellation ends the durable wait. |
| Structured Output | [structuredoutput](src/main/java/io/temporal/samples/googleadk/structuredoutput) | ADK supplies the model's output schema; the Workflow validates JSON and returns a typed `TravelPlan` or a non-retryable failure. |
| Multi-Agent | [multiagent](src/main/java/io/temporal/samples/googleadk/multiagent) | ADK's `ParallelAgent` starts planner and reviewer model Activities concurrently on Workflow threads. |
| Sandboxes | Unsupported | The plugin does not durably manage an agent-facing code execution environment. ADK code executors that perform host I/O cannot run on the Workflow thread. |
| Observability | Replay-safe ADK tracing unsupported | ADK uses global OpenTelemetry providers; this plugin has no replay suppression, so installing a tracing exporter duplicates spans during replay. Temporal history and UI inspection remain available. |

The unsupported entries describe the published integration's
[documented limits](https://github.com/temporalio/ai-integrations/blob/java/google-adk/v0.1.0/java/google-adk/README.md#limitations),
not an absence of the corresponding feature in ADK itself.

Additional examples:

- [longrunning](src/main/java/io/temporal/samples/googleadk/longrunning) uses the plugin's
  `ActivityTools.createLongRunning`, `Hitl.longRunningCalls`, and `Hitl.functionResponse` APIs.
  A submitted report remains pending until a correlated callback Signal delivers its result.
  This differs from approving an operation before it executes: it resumes an operation already started.
- [basic](src/main/java/io/temporal/samples/googleadk/basic) preserves the original single-turn example
  and its documentation snippets. Start with Agent Chat for the conversation-oriented entry point.

## Approve or reject a tool call

See [humanintheloop](src/main/java/io/temporal/samples/googleadk/humanintheloop) for CLI commands to
start, query, decide, and cancel. Query until `pendingConfirmations` contains a request before
sending `decide`. A decision applies to the currently pending batch; premature Signals are ignored, and the first
decision for a pending batch wins. Rejection never schedules `ShipOrder`.

## Continue a conversation as new

See [chat](src/main/java/io/temporal/samples/googleadk/chat) for CLI commands. After three turns the
Workflow exports its ADK session and carries queued messages and the latest reply into a new Run.
`finish` drains queued messages and returns the last reply. Continue-As-New bounds each Run's history;
the conversation snapshot itself still grows and must fit Temporal's payload limits.

## Test without provider credentials

```bash
./gradlew :googleadk:test :googleadk:spotlessCheck :googleadk:build
```

Tests use the published `ScriptedModel` or the same `OfflineModel` as the runnable Worker, and
`TestWorkflowEnvironment`. They need no provider credentials, external Temporal Service, or telemetry
backend. The MCP tests launch the bundled local subprocess.

Each supported scenario has a targeted test command in its README. Tests assert Activity scheduling,
retry behavior, function results, confirmation decisions and cancellation, schema rejection,
correlated callback completion, and session/queue recovery across Continue-As-New. Recorded histories
replay on a fresh Worker without repeating model or tool calls. `OfflineSamplesTest` runs every
scenario with `SampleWorker.register` and the actual offline provider.

The original `:googleadk:starter` task remains available for `basic`, `tools`, `multiagent`, `mcp`,
`approval`, and `chat`, and their message commands. The scenario READMEs use Temporal CLI directly.
External Java clients that exchange ADK `Session` or `Content` payloads must also register
`GoogleAdkPlugin` to use its payload converter; ordinary CLI JSON inputs need no Java client.

## Boundaries

- Put host clocks, files, network services, and nondeterministic tools in Activities or MCP. The
  weather Activity deliberately reads `Instant.now()` alongside illustrative, fixed conditions;
  it does not call a real weather provider. Shipping and report submission also return sample data.
- Do not override agent-level schedulers/executors or use RxJava blocking APIs inside Workflows.
- Session snapshots preserve session-scoped state and events, not app-, user-, or temporary state.
- A cancellation stops the Workflow's wait. It does not undo a shipment or cancel an external report
  already submitted; a real backend needs an explicit cancellation Activity.

See the [integration reference](https://github.com/temporalio/ai-integrations/tree/main/java/google-adk)
for the full API and limitations.
