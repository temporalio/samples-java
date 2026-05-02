package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class IncidentTriageWorkflowImpl implements IncidentTriageWorkflow {
  private Types.AlertPayload currentAlert;
  private Types.TriageResult result;

  @Override
  public Types.TriageResult run(Types.AlertPayload alert) {
    this.currentAlert = alert;
    // agenticHitl-shaped timeouts (matches lexicon-temporal's profile).
    ActivityOptions opts =
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofHours(8))
            .setHeartbeatTimeout(Duration.ofMinutes(2))
            // 1 attempt — AgenticSession heartbeat is the resume mechanism.
            .build();
    TriageActivity activity = Workflow.newActivityStub(TriageActivity.class, opts);
    this.result = activity.triageIncident(currentAlert);
    return this.result;
  }

  @Override
  public void alertUpdate(Types.AlertPayload alert) {
    this.currentAlert = alert;
  }

  @Override
  public Types.AlertPayload currentAlert() {
    return currentAlert;
  }

  @Override
  public Types.TriageResult triageResult() {
    return result;
  }
}
