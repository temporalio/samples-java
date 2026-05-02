package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.time.Instant;
import java.util.Map;

/**
 * CLI: approve / reject / trigger.
 *
 * <p>Listing pending approval workflows is left to the Temporal CLI: temporal workflow list --query
 * 'WorkflowType="ApprovalWorkflow" AND ExecutionStatus="Running"'
 */
public class Client {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Usage: client <approve|reject|trigger> ...");
      System.exit(1);
    }
    WorkflowClient client = makeClient();
    switch (args[0]) {
      case "approve":
        if (args.length < 3) {
          System.err.println("Usage: client approve <wfid> <reason>");
          System.exit(1);
        }
        decide(client, "approved", args[1], joinFrom(args, 2));
        break;
      case "reject":
        if (args.length < 3) {
          System.err.println("Usage: client reject <wfid> <reason>");
          System.exit(1);
        }
        decide(client, "rejected", args[1], joinFrom(args, 2));
        break;
      case "trigger":
        if (args.length < 3) {
          System.err.println("Usage: client trigger <alertname> <service>");
          System.exit(1);
        }
        trigger(client, args[1], args[2]);
        break;
      default:
        System.err.println("Unknown command: " + args[0]);
        System.exit(1);
    }
  }

  static WorkflowClient makeClient() {
    String address = mustEnv("TEMPORAL_ADDRESS");
    String namespace = mustEnv("TEMPORAL_NAMESPACE");
    String apiKey = mustEnv("TEMPORAL_API_KEY");
    WorkflowServiceStubs s =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(address)
                .addApiKey(() -> apiKey)
                .build());
    return WorkflowClient.newInstance(
        s, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
  }

  static void decide(WorkflowClient client, String decision, String workflowId, String reason) {
    ApprovalWorkflow stub = client.newWorkflowStub(ApprovalWorkflow.class, workflowId);
    Types.ApprovalResponse r = new Types.ApprovalResponse(decision, reason);
    stub.approvalDecision(r);
    System.out.println("signaled " + workflowId + ": " + decision + " — " + reason);
  }

  static void trigger(WorkflowClient client, String alertname, String service) {
    String taskQueue = System.getenv().getOrDefault("TEMPORAL_TASK_QUEUE", "triage-java");
    String wfId =
        "triage-"
            + alertname.toLowerCase(java.util.Locale.ROOT)
            + "-"
            + service.toLowerCase(java.util.Locale.ROOT);
    Types.AlertPayload alert = new Types.AlertPayload();
    alert.status = "firing";
    alert.labels =
        Map.of(
            "alertname",
            alertname,
            "service",
            service,
            "severity",
            "critical",
            "runbook",
            "synthetic");
    alert.annotations =
        Map.of(
            "summary",
            "Synthetic test alert for " + service,
            "description",
            "Triggered manually via Client to exercise the triage flow.");
    alert.startsAt = Instant.now().toString();
    IncidentTriageWorkflow stub =
        client.newWorkflowStub(
            IncidentTriageWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId(wfId).setTaskQueue(taskQueue).build());
    WorkflowClient.start(stub::run, alert);
    System.out.println("started triage workflow: " + wfId + " on " + taskQueue);
  }

  static String mustEnv(String name) {
    String v = System.getenv(name);
    if (v == null || v.isEmpty()) {
      System.err.println("missing env var: " + name);
      System.exit(1);
    }
    return v;
  }

  static String joinFrom(String[] arr, int from) {
    StringBuilder b = new StringBuilder();
    for (int i = from; i < arr.length; i++) {
      if (i > from) b.append(' ');
      b.append(arr[i]);
    }
    return b.toString();
  }
}
