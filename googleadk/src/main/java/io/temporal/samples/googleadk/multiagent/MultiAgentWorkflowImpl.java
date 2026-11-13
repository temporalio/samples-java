package io.temporal.samples.googleadk.multiagent;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.samples.googleadk.AgentSupport;
import java.util.List;

public class MultiAgentWorkflowImpl implements MultiAgentWorkflow {
  // @@@SNIPSTART java-google-adk-multiagent-workflow
  @Override
  public List<String> plan(String question) {
    LlmAgent planner =
        LlmAgent.builder()
            .name("planner")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Propose a short travel itinerary for the user's destination.")
            .build();
    LlmAgent reviewer =
        LlmAgent.builder()
            .name("reviewer")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("List practical travel considerations for the user's destination.")
            .build();
    ParallelAgent root =
        ParallelAgent.builder().name("travel_team").subAgents(planner, reviewer).build();
    // The runner supplies workflow threads for ADK's parallel fan-out.
    Runner runner = TemporalAdk.runnerBuilder().appName("travel").agent(root).build();
    return TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(question)))
        .stream()
        .flatMap(event -> event.content().stream())
        .map(Content::text)
        .filter(text -> text != null && !text.isEmpty())
        .sorted()
        .toList();
  }
  // @@@SNIPEND
}
