# Stateless MCP over stdio

The agent lists and calls `echo` on a bundled Java MCP server. The Worker starts the subprocess on
first use; Workflows only reference its registered toolset name. Both protocol operations are
Activities. No Node installation, remote MCP service, or listening port is required.

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
temporal workflow execute --type McpWorkflow --task-queue google-adk-samples --input '"Hello MCP"'
```

Both modes echo `Hello MCP` after calling the real local server. History contains `ListMcpTools`,
`InvokeModel`, `CallMcpTool`, and a final `InvokeModel`, all under the `googleadk.` prefix.
`cacheTools(true)` avoids listing again during the second model turn. The plugin closes the toolset
and subprocess when the Worker factory shuts down. Tests exercise stdio and replay the history.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.McpWorkflowTest
```
