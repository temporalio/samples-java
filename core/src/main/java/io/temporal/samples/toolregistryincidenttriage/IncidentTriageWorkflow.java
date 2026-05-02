package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface IncidentTriageWorkflow {
  @WorkflowMethod
  Types.TriageResult run(Types.AlertPayload alert);

  @SignalMethod(name = "alert-update")
  void alertUpdate(Types.AlertPayload alert);

  @QueryMethod(name = "current-alert")
  Types.AlertPayload currentAlert();

  @QueryMethod(name = "triage-result")
  Types.TriageResult triageResult();
}
