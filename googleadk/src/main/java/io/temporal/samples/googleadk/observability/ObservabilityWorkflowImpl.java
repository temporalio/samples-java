package io.temporal.samples.googleadk.observability;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.activity.ActivityOptions;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.googleadk.tool.ActivityTools;
import io.temporal.samples.googleadk.AgentSupport;
import io.temporal.samples.googleadk.tools.WeatherActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ObservabilityWorkflowImpl implements ObservabilityWorkflow {
  // @@@SNIPSTART java-google-adk-observability-workflow
  @Override
  public String observe(String prompt) {
    WeatherActivities weather =
        Workflow.newActivityStub(
            WeatherActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());
    LlmAgent agent =
        LlmAgent.builder()
            .name("observed_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Use getWeather and summarize its result.")
            .tools(ActivityTools.create(weather, "getWeather"))
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("observability").agent(agent).build();
    return AgentSupport.lastText(
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(prompt))));
  }
  // @@@SNIPEND
}
