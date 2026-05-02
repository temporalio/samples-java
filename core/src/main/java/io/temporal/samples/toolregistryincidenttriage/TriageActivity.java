package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TriageActivity {
  @ActivityMethod(name = "triage_incident_activity")
  Types.TriageResult triageIncident(Types.AlertPayload alert);
}
