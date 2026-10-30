# Workflow and Activity tools

The agent calls `getWeather` as a Temporal Activity, then `toFahrenheit` as a deterministic function
on the Workflow thread. The Activity reads a host timestamp and returns illustrative fixed weather;
replace its body with provider I/O for real observations. The conversion needs no Activity.

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
temporal workflow execute --type ToolsWorkflow --task-queue google-adk-samples --input '"What is the weather in Chicago? Give Celsius and Fahrenheit."'
```

Offline mode returns `Chicago is sunny: 20 C / 68.0 F.`. Live Gemini uses the same tools. History shows
three `googleadk.InvokeModel` Activities and one `GetWeather`, with no Activity for `toFahrenheit`.
The tests inject a transient weather failure, verify its retry and both tool results, then replay
without repeating the observation.

Test without provider credentials or an external Service:

```bash
./gradlew :googleadk:test --tests io.temporal.samples.googleadk.ToolsWorkflowTest
```
