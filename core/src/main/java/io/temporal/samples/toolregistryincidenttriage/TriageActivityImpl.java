package io.temporal.samples.toolregistryincidenttriage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.toolregistry.AgenticSession;
import io.temporal.toolregistry.AnthropicConfig;
import io.temporal.toolregistry.AnthropicProvider;
import io.temporal.toolregistry.ToolDefinition;
import io.temporal.toolregistry.ToolHandler;
import io.temporal.toolregistry.ToolRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Java port of the triage activity. Mirrors workers/typescript and workers/python.
 *
 * <p>Structure: {@link #buildTriageRegistry(Types.AlertPayload, AgenticSession, TriageDeps)} is the
 * testable seam — pure modulo deps. {@link #triageIncident(Types.AlertPayload)} composes
 * runWithSession + the registry + the Anthropic provider.
 */
public class TriageActivityImpl implements TriageActivity {

  public static final String SYSTEM_PROMPT =
      "You are an SRE on-call agent triaging a production alert.\n\n"
          + "You have these tools (sourced from MCP sidecars + per-language helpers):\n"
          + "  - prometheus_query(query)            instant PromQL query\n"
          + "  - prometheus_query_range(query, start, end, step)\n"
          + "  - prometheus_alerts()                what is currently firing\n"
          + "  - kubectl_get(resource, namespace?)  list K8s resources\n"
          + "  - kubectl_describe(resource, name, namespace?)\n"
          + "  - kubectl_logs(pod, namespace, tail?)\n"
          + "  - propose_remediation(action, justification)   record but do NOT execute\n"
          + "  - request_human_approval(message, diagnosis, proposedAction)\n"
          + "                                       blocks until operator says approve|reject\n"
          + "  - execute_remediation(action)        ONLY callable AFTER approval was approved.\n"
          + "                                       Pass the same action you got approved.\n"
          + "  - report_resolved(summary)           ends the loop with status=resolved\n"
          + "  - report_unresolved(summary)         ends the loop with status=unresolved\n\n"
          + "Workflow:\n"
          + "  1. Read the alert. Use prometheus_query to confirm the symptom is currently true.\n"
          + "  2. Use kubectl_get/describe/logs and prometheus_query_range to find root cause.\n"
          + "  3. propose_remediation with a specific action (e.g., \"kubectl rollout restart deploy/api -n demo-app\").\n"
          + "  4. request_human_approval, attaching your diagnosis and the proposed action.\n"
          + "  5. If approved: execute_remediation, then prometheus_query to verify the symptom is gone, then report_resolved.\n"
          + "  6. If rejected: report_unresolved with the operator's reason.\n\n"
          + "Be terse. Conversation history is heartbeated to Temporal — keep tool inputs short.";

  /** Pluggable I/O for the activity. Tests substitute their own. */
  public interface TriageDeps {
    List<McpToolInfo> mcpListTools(String baseUrl) throws Exception;

    String mcpCallTool(String baseUrl, String name, Map<String, Object> args) throws Exception;

    Types.ApprovalResponse requestHumanApproval(Types.AlertPayload alert, Types.ApprovalRequest req)
        throws Exception;

    ShellResult execShellCommand(String cmd) throws Exception;
  }

  public static final class McpToolInfo {
    public String name;
    public String description;
    public Map<String, Object> inputSchema;
  }

  public static final class ShellResult {
    public String stdout = "";
    public String stderr = "";
  }

  /** Default deps wired to real MCP HTTP, real shell exec, real Temporal client for HITL. */
  public static TriageDeps defaultDeps() {
    return new TriageDeps() {
      private final HttpClient http =
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      private final ObjectMapper mapper = new ObjectMapper();

      @Override
      public List<McpToolInfo> mcpListTools(String baseUrl) throws Exception {
        String body = mcpRpc(baseUrl, "tools/list", null);
        Map<?, ?> root = mapper.readValue(body, Map.class);
        Map<?, ?> result = (Map<?, ?>) root.get("result");
        List<McpToolInfo> out = new ArrayList<>();
        if (result == null) return out;
        List<?> tools = (List<?>) result.get("tools");
        if (tools == null) return out;
        for (Object t : tools) {
          Map<?, ?> m = (Map<?, ?>) t;
          McpToolInfo info = new McpToolInfo();
          info.name = (String) m.get("name");
          Object descRaw = m.get("description");
          info.description = descRaw != null ? (String) descRaw : "";
          @SuppressWarnings("unchecked")
          Map<String, Object> schema = (Map<String, Object>) m.get("inputSchema");
          info.inputSchema = schema != null ? schema : Map.of("type", "object");
          out.add(info);
        }
        return out;
      }

      @Override
      public String mcpCallTool(String baseUrl, String name, Map<String, Object> args)
          throws Exception {
        String body = mcpRpc(baseUrl, "tools/call", Map.of("name", name, "arguments", args));
        Map<?, ?> root = mapper.readValue(body, Map.class);
        if (root.get("error") != null) {
          return "MCP error: " + ((Map<?, ?>) root.get("error")).get("message");
        }
        Map<?, ?> result = (Map<?, ?>) root.get("result");
        List<?> blocks = (List<?>) result.get("content");
        StringBuilder sb = new StringBuilder();
        for (Object b : blocks) {
          Map<?, ?> m = (Map<?, ?>) b;
          if (sb.length() > 0) sb.append("\n");
          Object textRaw = m.get("text");
          if (textRaw != null) sb.append(textRaw);
        }
        return sb.toString();
      }

      private String mcpRpc(String baseUrl, String method, Object params) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", System.currentTimeMillis());
        body.put("method", method);
        if (params != null) body.put("params", params);
        HttpRequest req =
            HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
      }

      @Override
      public Types.ApprovalResponse requestHumanApproval(
          Types.AlertPayload alert, Types.ApprovalRequest req) throws Exception {
        return realRequestHumanApproval(alert, req);
      }

      @Override
      public ShellResult execShellCommand(String cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
          p.destroyForcibly();
          throw new IOException("exec timed out: " + cmd);
        }
        ShellResult r = new ShellResult();
        r.stdout =
            new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        r.stderr =
            new String(p.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return r;
      }
    };
  }

  /** Build a populated registry plus a getter for the final result. Pure modulo deps. */
  public static RegistryAndResult buildTriageRegistry(
      Types.AlertPayload alert, AgenticSession session, TriageDeps deps) {
    ToolRegistry registry = new ToolRegistry();
    String promMcp = envOr("MCP_PROMETHEUS_URL", "http://localhost:7071/");
    String k8sMcp = envOr("MCP_KUBERNETES_URL", "http://localhost:7072/");

    // MCP-sourced tools.
    registerMcpTools(registry, promMcp, deps);
    registerMcpTools(registry, k8sMcp, deps);

    // Per-language tools.
    List<Types.ProposedRemediation> remediations = new ArrayList<>();
    AtomicReference<String> approvedAction = new AtomicReference<>(null);
    AtomicReference<Types.TriageResult> finalResult = new AtomicReference<>(null);

    registry.register(
        ToolDefinition.builder()
            .name("propose_remediation")
            .description("Record a remediation you would apply. Does NOT execute it.")
            .inputSchema(
                Map.of(
                    "type", "object",
                    "properties",
                        Map.of(
                            "action", Map.of("type", "string"),
                            "justification", Map.of("type", "string")),
                    "required", List.of("action", "justification")))
            .build(),
        (ToolHandler)
            input -> {
              Types.ProposedRemediation r =
                  new Types.ProposedRemediation(
                      String.valueOf(input.get("action")),
                      String.valueOf(input.get("justification")));
              remediations.add(r);
              session.addResult(
                  Map.of(
                      "kind", "remediation", "action", r.action, "justification", r.justification));
              return "recorded";
            });

    registry.register(
        ToolDefinition.builder()
            .name("request_human_approval")
            .description("Block until operator decides. Returns JSON {decision, reason}.")
            .inputSchema(
                Map.of(
                    "type", "object",
                    "properties",
                        Map.of(
                            "message", Map.of("type", "string"),
                            "diagnosis", Map.of("type", "string"),
                            "proposedAction", Map.of("type", "string")),
                    "required", List.of("message", "diagnosis", "proposedAction")))
            .build(),
        input -> {
          Types.ApprovalRequest req = new Types.ApprovalRequest();
          req.message = String.valueOf(input.get("message"));
          req.diagnosis = String.valueOf(input.get("diagnosis"));
          req.proposedAction = String.valueOf(input.get("proposedAction"));
          Types.ApprovalResponse resp = deps.requestHumanApproval(alert, req);
          if ("approved".equals(resp.decision)) {
            approvedAction.set(req.proposedAction);
          }
          session.addResult(
              Map.of("kind", "approval", "decision", resp.decision, "reason", resp.reason));
          return "{\"decision\":\""
              + resp.decision
              + "\",\"reason\":\""
              + jsonEscape(resp.reason)
              + "\"}";
        });

    registry.register(
        ToolDefinition.builder()
            .name("execute_remediation")
            .description(
                "Execute the previously-approved action. Errors if no approval has been granted.")
            .inputSchema(
                Map.of(
                    "type", "object",
                    "properties", Map.of("action", Map.of("type", "string")),
                    "required", List.of("action")))
            .build(),
        input -> {
          String action = String.valueOf(input.get("action"));
          if (approvedAction.get() == null) {
            return "ERROR: no approval has been granted. Call request_human_approval first.";
          }
          if (!action.equals(approvedAction.get())) {
            return "ERROR: requested action does not match approved action. Approved: "
                + approvedAction.get();
          }
          ShellResult r = deps.execShellCommand(action);
          session.addResult(
              Map.of(
                  "kind",
                  "executed",
                  "action",
                  action,
                  "stdout",
                  clip(r.stdout, 2000),
                  "stderr",
                  clip(r.stderr, 2000)));
          String out = !r.stdout.isEmpty() ? r.stdout : (!r.stderr.isEmpty() ? r.stderr : "ok");
          return clip(out, 4000);
        });

    registry.register(
        ToolDefinition.builder()
            .name("report_resolved")
            .description("Ends the loop with status=resolved.")
            .inputSchema(
                Map.of(
                    "type", "object",
                    "properties", Map.of("summary", Map.of("type", "string")),
                    "required", List.of("summary")))
            .build(),
        input -> {
          Types.TriageResult r = new Types.TriageResult();
          r.status = "resolved";
          r.summary = String.valueOf(input.get("summary"));
          r.remediations = new ArrayList<>(remediations);
          finalResult.set(r);
          session.addResult(Map.of("kind", "final", "status", r.status, "summary", r.summary));
          return "ok";
        });

    registry.register(
        ToolDefinition.builder()
            .name("report_unresolved")
            .description("Ends the loop with status=unresolved.")
            .inputSchema(
                Map.of(
                    "type", "object",
                    "properties", Map.of("summary", Map.of("type", "string")),
                    "required", List.of("summary")))
            .build(),
        input -> {
          Types.TriageResult r = new Types.TriageResult();
          r.status = "unresolved";
          r.summary = String.valueOf(input.get("summary"));
          r.remediations = new ArrayList<>(remediations);
          finalResult.set(r);
          session.addResult(Map.of("kind", "final", "status", r.status, "summary", r.summary));
          return "ok";
        });

    return new RegistryAndResult(registry, finalResult::get);
  }

  private static void registerMcpTools(ToolRegistry registry, String baseUrl, TriageDeps deps) {
    try {
      for (McpToolInfo t : deps.mcpListTools(baseUrl)) {
        final String name = t.name;
        Map<String, Object> schema =
            t.inputSchema != null ? t.inputSchema : Map.of("type", "object");
        registry.register(
            ToolDefinition.builder()
                .name(name)
                .description(t.description == null ? "" : t.description)
                .inputSchema(schema)
                .build(),
            (ToolHandler) input -> deps.mcpCallTool(baseUrl, name, input));
      }
    } catch (Exception ignored) {
      // MCP server unreachable in testing or at boot — proceed without these tools.
    }
  }

  public static String buildPrompt(Types.AlertPayload alert) {
    return "Alert fired: "
        + getOr(alert.labels, "alertname", "unknown")
        + " on "
        + getOr(alert.labels, "service", "unknown")
        + ".\nSummary: "
        + getOr(alert.annotations, "summary", "(none)")
        + "\nDescription: "
        + getOr(alert.annotations, "description", "(none)")
        + "\nRunbook hint: "
        + getOr(alert.labels, "runbook", "(none)")
        + "\n\nInvestigate, propose, get approval, and either fix or report unresolved.";
  }

  public static final class RegistryAndResult {
    public final ToolRegistry registry;
    public final java.util.function.Supplier<Types.TriageResult> getResult;

    public RegistryAndResult(ToolRegistry r, java.util.function.Supplier<Types.TriageResult> g) {
      this.registry = r;
      this.getResult = g;
    }
  }

  @Override
  public Types.TriageResult triageIncident(Types.AlertPayload alert) {
    TriageDeps deps = defaultDeps();
    AtomicReference<Types.TriageResult> out = new AtomicReference<>();
    try {
      AgenticSession.runWithSession(
          session -> {
            RegistryAndResult rar = buildTriageRegistry(alert, session, deps);
            AnthropicConfig cfg =
                AnthropicConfig.builder().apiKey(System.getenv("ANTHROPIC_API_KEY")).build();
            AnthropicProvider provider = new AnthropicProvider(cfg, rar.registry, SYSTEM_PROMPT);
            session.runToolLoop(provider, rar.registry, buildPrompt(alert));
            Types.TriageResult final_ = rar.getResult.get();
            if (final_ == null) {
              throw new RuntimeException(
                  "Agent ended the loop without calling report_resolved or report_unresolved");
            }
            out.set(final_);
          });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return out.get();
  }

  static Types.ApprovalResponse realRequestHumanApproval(
      Types.AlertPayload alert, Types.ApprovalRequest req) {
    String address = System.getenv("TEMPORAL_ADDRESS");
    String namespace = System.getenv("TEMPORAL_NAMESPACE");
    String apiKey = System.getenv("TEMPORAL_API_KEY");
    String taskQueue = envOr("TEMPORAL_TASK_QUEUE", "triage-java");
    if (address == null || namespace == null || apiKey == null) {
      throw new IllegalStateException(
          "Missing TEMPORAL_ADDRESS / TEMPORAL_NAMESPACE / TEMPORAL_API_KEY");
    }
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(address)
                .addApiKey(() -> apiKey)
                .build());
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            io.temporal.client.WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    String key =
        (getOr(alert.labels, "alertname", "unknown")
                + "-"
                + getOr(alert.labels, "service", "unknown"))
            .toLowerCase(java.util.Locale.ROOT);
    String wfId = "approval-" + key;

    ApprovalWorkflow stub =
        client.newWorkflowStub(
            ApprovalWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(wfId)
                .setTaskQueue(taskQueue)
                // If the activity retries while the approval workflow is still running,
                // attach to the existing one rather than starting a new approval. The
                // operator should not get a second prompt for the same incident.
                .setWorkflowIdConflictPolicy(
                    WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                .build());

    // Atomic signal-with-start: send the request signal in the same call that starts
    // the workflow. Avoids the race where a separate signal could arrive before the
    // workflow registers its handler.
    WorkflowStub.fromTyped(stub)
        .signalWithStart("approval-request", new Object[] {req}, new Object[] {key});
    return stub.run(key); // already running; this returns the in-flight result
  }

  static String envOr(String n, String d) {
    String v = System.getenv(n);
    return v == null || v.isEmpty() ? d : v;
  }

  static String getOr(Map<String, String> m, String k, String d) {
    if (m == null) return d;
    String v = m.get(k);
    return v == null || v.isEmpty() ? d : v;
  }

  static String clip(String s, int n) {
    return s.length() <= n ? s : s.substring(0, n);
  }

  static String jsonEscape(String s) {
    return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }
}
