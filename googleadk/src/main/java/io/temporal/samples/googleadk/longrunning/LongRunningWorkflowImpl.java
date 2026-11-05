package io.temporal.samples.googleadk.longrunning;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.Part;
import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.googleadk.tool.ActivityTools;
import io.temporal.googleadk.tool.Hitl;
import io.temporal.samples.googleadk.AgentSupport;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class LongRunningWorkflowImpl implements LongRunningWorkflow {
  private String callId = "";
  private String result;

  // @@@SNIPSTART java-google-adk-longrunning-workflow
  @Override
  public String prepare(String request) {
    ReportActivities reports =
        Workflow.newActivityStub(
            ReportActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());
    LlmAgent agent =
        LlmAgent.builder()
            .name("report_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Start exactly one report with startReport. Wait for its final result.")
            .tools(ActivityTools.createLongRunning(reports, "startReport"))
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("reports").agent(agent).build();
    List<Event> events =
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Content.fromParts(Part.fromText(request)));
    List<FunctionCall> pending = Hitl.longRunningCalls(events);
    if (pending.size() != 1) {
      throw ApplicationFailure.newNonRetryableFailure(
          "Expected exactly one pending report.", "InvalidReportRequest");
    }
    FunctionCall call = pending.get(0);
    callId = call.id().orElseThrow();
    Workflow.await(() -> result != null);
    events =
        TemporalAdk.run(
            runner,
            AgentSupport.USER,
            AgentSupport.SESSION,
            Hitl.functionResponse(call, Map.of("status", "done", "result", result)));
    callId = "";
    return AgentSupport.lastText(events);
  }

  // @@@SNIPEND

  @Override
  public String pendingCall() {
    return callId;
  }

  @Override
  public void complete(String functionCallId, String value) {
    if (!callId.isEmpty() && callId.equals(functionCallId) && result == null && value != null) {
      result = value;
    }
  }
}
