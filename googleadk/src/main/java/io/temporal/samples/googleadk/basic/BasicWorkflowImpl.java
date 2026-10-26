package io.temporal.samples.googleadk.basic;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.samples.googleadk.AgentSupport;
import java.util.List;

public class BasicWorkflowImpl implements BasicWorkflow {
  // @@@SNIPSTART java-google-adk-basic-workflow
  @Override
  public String ask(String question) {
    LlmAgent agent =
        LlmAgent.builder()
            .name("assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Answer concisely.")
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("basic").agent(agent).build();
    List<Event> events =
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(question)));
    return AgentSupport.lastText(events);
  }
  // @@@SNIPEND
}
