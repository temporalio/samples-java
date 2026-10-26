package io.temporal.samples.googleadk.humanintheloop;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.googleadk.tool.ActivityTools;
import io.temporal.googleadk.tool.Hitl;
import io.temporal.samples.googleadk.AgentSupport;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {
  private List<Hitl.PendingConfirmation> pending = List.of();
  private Boolean decision;

  // @@@SNIPSTART java-google-adk-approval-workflow
  @Override
  public String fulfill(String question) {
    OrderActivities orders =
        Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    LlmAgent agent =
        LlmAgent.builder()
            .name("fulfillment_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction(
                "Use shipOrder when asked to ship an order. Respect rejected confirmations.")
            .tools(ActivityTools.create(orders, "shipOrder", true))
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("approval").agent(agent).build();
    List<Event> events =
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(question)));
    pending = Hitl.pendingConfirmations(events);
    while (!pending.isEmpty()) {
      Workflow.await(() -> decision != null);
      Content response =
          Hitl.confirmationResponse(
              pending.stream()
                  .map(p -> new Hitl.ConfirmationDecision(p.functionCallId(), decision))
                  .toArray(Hitl.ConfirmationDecision[]::new));
      decision = null;
      pending = List.of();
      events = TemporalAdk.run(runner, AgentSupport.USER, AgentSupport.SESSION, response);
      pending = Hitl.pendingConfirmations(events);
    }
    return AgentSupport.lastText(events);
  }

  // @@@SNIPEND

  @Override
  public List<Hitl.PendingConfirmation> pendingConfirmations() {
    return List.copyOf(pending);
  }

  @Override
  public void decide(boolean confirmed) {
    decision = confirmed;
  }
}
