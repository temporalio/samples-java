package io.temporal.samples.googleadk.streaming;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.samples.googleadk.AgentSupport;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import java.time.Duration;
import java.util.List;

public class StreamingWorkflowImpl implements StreamingWorkflow {
  private final WorkflowStream output;
  private boolean acknowledged;

  @WorkflowInit
  public StreamingWorkflowImpl(String prompt) {
    output = WorkflowStream.newInstance(null);
  }

  // @@@SNIPSTART java-google-adk-streaming-workflow
  @Override
  public List<ModelChunk> stream(String prompt) {
    LlmAgent agent =
        LlmAgent.builder()
            .name("streaming_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Answer concisely.")
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("streaming").agent(agent).build();
    List<Event> events =
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(prompt)),
            RunConfig.builder()
                .streamingMode(RunConfig.StreamingMode.SSE)
                .autoCreateSession(true)
                .build());
    List<ModelChunk> chunks =
        events.stream()
            .filter(event -> event.content().isPresent())
            .map(
                event ->
                    new ModelChunk(
                        event.partial().orElse(false) ? "partial" : "final",
                        event.content().map(Content::text).orElse("")))
            .toList();
    for (ModelChunk chunk : chunks) output.topic("model").publish(chunk);
    output.topic("model").publish(new ModelChunk("complete", AgentSupport.lastText(events)));
    Workflow.await(Duration.ofSeconds(10), () -> acknowledged);
    return chunks;
  }

  // @@@SNIPEND

  @Override
  public void acknowledge() {
    acknowledged = true;
  }
}
