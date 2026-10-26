package io.temporal.samples.googleadk.mcp;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.mcp.TemporalMcpToolset;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.samples.googleadk.AgentSupport;

public class McpWorkflowImpl implements McpWorkflow {
  // @@@SNIPSTART java-google-adk-mcp-workflow
  @Override
  public String echo(String message) {
    LlmAgent agent =
        LlmAgent.builder()
            .name("echo_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Call echo with the user's message, then return the echoed text.")
            .tools(TemporalMcpToolset.newBuilder("echo").cacheTools(true).build())
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("echo").agent(agent).build();
    return AgentSupport.lastText(
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(message))));
  }
  // @@@SNIPEND
}
