package io.temporal.samples.googleadk.tools;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.Runner;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.googleadk.tool.ActivityTools;
import io.temporal.samples.googleadk.AgentSupport;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ToolsWorkflowImpl implements ToolsWorkflow {
  // @@@SNIPSTART java-google-adk-tools-workflow
  @Override
  public String ask(String question) {
    WeatherActivities weather =
        Workflow.newActivityStub(
            WeatherActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    LlmAgent agent =
        LlmAgent.builder()
            .name("weather_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction(
                "Use getWeather for conditions and Celsius temperature, then toFahrenheit to"
                    + " convert the temperature. Report the conditions and both temperatures.")
            .tools(
                ActivityTools.create(weather, "getWeather"),
                FunctionTool.create(TemperatureTools.class, "toFahrenheit"))
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("tools").agent(agent).build();
    return AgentSupport.lastText(
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(question))));
  }
  // @@@SNIPEND
}
