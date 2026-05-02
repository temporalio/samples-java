package io.temporal.samples.toolregistryincidenttriage;

import java.util.List;
import java.util.Map;

/** Shared types for the Java triage worker. POJOs with public fields for Jackson. */
public final class Types {

  public static final class AlertPayload {
    public String status;
    public Map<String, String> labels;
    public Map<String, String> annotations;
    public String startsAt;
    public String endsAt;
    public String fingerprint;
  }

  public static final class ProposedRemediation {
    public String action;
    public String justification;

    public ProposedRemediation() {}

    public ProposedRemediation(String action, String justification) {
      this.action = action;
      this.justification = justification;
    }
  }

  public static final class TriageResult {
    public String status; // "resolved" | "unresolved"
    public String summary;
    public List<ProposedRemediation> remediations;
  }

  public static final class ApprovalRequest {
    public String message;
    public String diagnosis;
    public String proposedAction;
  }

  public static final class ApprovalResponse {
    public String decision; // "approved" | "rejected"
    public String reason;

    public ApprovalResponse() {}

    public ApprovalResponse(String decision, String reason) {
      this.decision = decision;
      this.reason = reason;
    }
  }

  private Types() {}
}
