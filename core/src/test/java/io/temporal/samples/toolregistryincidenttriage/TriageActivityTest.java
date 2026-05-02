package io.temporal.samples.toolregistryincidenttriage;

import static org.junit.jupiter.api.Assertions.*;

import io.temporal.samples.toolregistryincidenttriage.TriageActivityImpl.McpToolInfo;
import io.temporal.samples.toolregistryincidenttriage.TriageActivityImpl.RegistryAndResult;
import io.temporal.samples.toolregistryincidenttriage.TriageActivityImpl.ShellResult;
import io.temporal.samples.toolregistryincidenttriage.TriageActivityImpl.TriageDeps;
import io.temporal.toolregistry.AgenticSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for buildTriageRegistry. Drives the registry directly via dispatch — bypasses
 * runWithSession and the LLM provider. Mirrors the TS / Python / Go suites.
 */
class TriageActivityTest {

  private Types.AlertPayload makeAlert() {
    Types.AlertPayload a = new Types.AlertPayload();
    a.status = "firing";
    a.labels =
        Map.of("alertname", "HighLatencyP99", "service", "api", "runbook", "rollback-or-scale");
    a.annotations = Map.of("summary", "P99 > 1s", "description", "P99 above threshold for 1m.");
    a.startsAt = Instant.now().toString();
    return a;
  }

  private TriageDeps makeDeps() {
    return new TriageDeps() {
      @Override
      public List<McpToolInfo> mcpListTools(String baseUrl) {
        McpToolInfo t = new McpToolInfo();
        if (baseUrl.contains("7071")) {
          t.name = "prometheus_query";
          t.description = "instant PromQL query";
          t.inputSchema =
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of("query", Map.of("type", "string")),
                  "required",
                  List.of("query"));
        } else {
          t.name = "kubectl_describe";
          t.description = "describe a k8s resource";
          t.inputSchema =
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "resource", Map.of("type", "string"),
                      "name", Map.of("type", "string"),
                      "namespace", Map.of("type", "string")),
                  "required",
                  List.of("resource", "name"));
        }
        return List.of(t);
      }

      @Override
      public String mcpCallTool(String baseUrl, String name, Map<String, Object> args) {
        return "(mocked " + name + ")";
      }

      @Override
      public Types.ApprovalResponse requestHumanApproval(
          Types.AlertPayload a, Types.ApprovalRequest r) {
        return new Types.ApprovalResponse("approved", "default-mock");
      }

      @Override
      public ShellResult execShellCommand(String cmd) {
        ShellResult r = new ShellResult();
        r.stdout = "(mocked exec: " + cmd + ")";
        return r;
      }
    };
  }

  private record Call(String name, Map<String, Object> input) {}

  private record DriveResult(Types.TriageResult result, List<Map<String, Object>> sessionResults) {}

  private DriveResult drive(TriageDeps deps, Call... calls) throws Exception {
    AgenticSession session = new AgenticSession();
    RegistryAndResult rar = TriageActivityImpl.buildTriageRegistry(makeAlert(), session, deps);
    for (Call c : calls) {
      rar.registry.dispatch(c.name(), c.input());
    }
    return new DriveResult(rar.getResult.get(), session.getResults());
  }

  @Test
  void happyPathResolved() throws Exception {
    AtomicInteger approvalCalls = new AtomicInteger(0);
    TriageDeps deps =
        override(
            makeDeps(),
            b ->
                b.requestHumanApproval =
                    (a, r) -> {
                      approvalCalls.incrementAndGet();
                      return new Types.ApprovalResponse("approved", "go ahead");
                    });
    String action = "kubectl rollout restart deploy/api -n demo-app";

    DriveResult dr =
        drive(
            deps,
            new Call("prometheus_query", Map.of("query", "up{service='api'}")),
            new Call(
                "kubectl_describe",
                Map.of("resource", "pod", "name", "api-xyz", "namespace", "demo-app")),
            new Call(
                "propose_remediation",
                Map.of("action", action, "justification", "leak; restart reclaims memory")),
            new Call(
                "request_human_approval",
                Map.of(
                    "message",
                    "Restart api?",
                    "diagnosis",
                    "memory leak",
                    "proposedAction",
                    action)),
            new Call("execute_remediation", Map.of("action", action)),
            new Call("report_resolved", Map.of("summary", "restarted; latency normal")));

    assertNotNull(dr.result());
    assertEquals("resolved", dr.result().status);
    assertTrue(dr.result().summary.contains("restart"));
    assertEquals(1, dr.result().remediations.size());
    assertEquals(action, dr.result().remediations.get(0).action);
    assertEquals(1, approvalCalls.get());

    List<String> kinds = dr.sessionResults().stream().map(r -> (String) r.get("kind")).toList();
    assertEquals(List.of("remediation", "approval", "executed", "final"), kinds);
  }

  @Test
  void rejectedApprovalUnresolved() throws Exception {
    TriageDeps deps =
        override(
            makeDeps(),
            b ->
                b.requestHumanApproval =
                    (a, r) ->
                        new Types.ApprovalResponse("rejected", "off-hours; defer until tomorrow"));

    DriveResult dr =
        drive(
            deps,
            new Call(
                "propose_remediation",
                Map.of("action", "kubectl scale ...", "justification", "transient")),
            new Call(
                "request_human_approval",
                Map.of(
                    "message",
                    "Scale?",
                    "diagnosis",
                    "transient",
                    "proposedAction",
                    "kubectl scale ...")),
            new Call("report_unresolved", Map.of("summary", "operator deferred")));

    assertEquals("unresolved", dr.result().status);
    assertTrue(dr.result().summary.contains("deferred"));
    Map<String, Object> approval =
        dr.sessionResults().stream()
            .filter(r -> "approval".equals(r.get("kind")))
            .findFirst()
            .orElse(null);
    assertNotNull(approval);
    assertEquals("rejected", approval.get("decision"));
    assertTrue(((String) approval.get("reason")).contains("off-hours"));
  }

  @Test
  void executeRefusesWithoutApproval() throws Exception {
    AtomicReference<String> executedCmd = new AtomicReference<>(null);
    TriageDeps deps =
        override(
            makeDeps(),
            b ->
                b.execShellCommand =
                    cmd -> {
                      executedCmd.set(cmd);
                      ShellResult r = new ShellResult();
                      r.stdout = "ran";
                      return r;
                    });

    DriveResult dr =
        drive(
            deps,
            new Call("execute_remediation", Map.of("action", "rm -rf /")),
            new Call("report_unresolved", Map.of("summary", "tried to skip approval")));

    assertEquals("unresolved", dr.result().status);
    assertNull(executedCmd.get(), "ExecShellCommand should not have been called");
  }

  @Test
  void executeRefusesWhenActionDoesNotMatch() throws Exception {
    AtomicReference<String> executedCmd = new AtomicReference<>(null);
    TriageDeps deps =
        override(
            makeDeps(),
            b -> {
              b.requestHumanApproval = (a, r) -> new Types.ApprovalResponse("approved", "ok");
              b.execShellCommand =
                  cmd -> {
                    executedCmd.set(cmd);
                    ShellResult r = new ShellResult();
                    r.stdout = "ran";
                    return r;
                  };
            });

    DriveResult dr =
        drive(
            deps,
            new Call(
                "propose_remediation",
                Map.of("action", "kubectl restart api", "justification", "x")),
            new Call(
                "request_human_approval",
                Map.of(
                    "message",
                    "Restart?",
                    "diagnosis",
                    "x",
                    "proposedAction",
                    "kubectl restart api")),
            new Call(
                "execute_remediation", Map.of("action", "kubectl scale deploy/api --replicas=10")),
            new Call("report_unresolved", Map.of("summary", "guard tripped")));

    assertEquals("unresolved", dr.result().status);
    assertNull(
        executedCmd.get(),
        "ExecShellCommand should not have been called when action did not match");
  }

  @Test
  void mcpToolsRegistered() throws Exception {
    AgenticSession session = new AgenticSession();
    RegistryAndResult rar =
        TriageActivityImpl.buildTriageRegistry(makeAlert(), session, makeDeps());
    List<String> names = new ArrayList<>();
    rar.registry.toAnthropic().forEach(t -> names.add((String) t.get("name")));
    for (String want :
        Arrays.asList(
            "prometheus_query",
            "kubectl_describe",
            "propose_remediation",
            "request_human_approval",
            "execute_remediation",
            "report_resolved",
            "report_unresolved")) {
      assertTrue(names.contains(want), "missing tool: " + want);
    }
  }

  @Test
  void mcpDispatchForwardsToSidecar() throws Exception {
    record Recorded(String url, String name, Map<String, Object> args) {}
    List<Recorded> calls = new ArrayList<>();
    TriageDeps base = makeDeps();
    TriageDeps deps =
        new TriageDeps() {
          @Override
          public List<McpToolInfo> mcpListTools(String url) throws Exception {
            return base.mcpListTools(url);
          }

          @Override
          public String mcpCallTool(String url, String name, Map<String, Object> args) {
            calls.add(new Recorded(url, name, args));
            return "result for " + name;
          }

          @Override
          public Types.ApprovalResponse requestHumanApproval(
              Types.AlertPayload a, Types.ApprovalRequest r) throws Exception {
            return base.requestHumanApproval(a, r);
          }

          @Override
          public ShellResult execShellCommand(String cmd) throws Exception {
            return base.execShellCommand(cmd);
          }
        };

    drive(
        deps,
        new Call("prometheus_query", Map.of("query", "up{}")),
        new Call("report_unresolved", Map.of("summary", "test")));

    assertEquals(1, calls.size());
    assertEquals("prometheus_query", calls.get(0).name());
    assertTrue(calls.get(0).url().contains("7071"));
    assertEquals("up{}", calls.get(0).args().get("query"));
  }

  /** Mutable bag for overriding dep functions in test cases without writing 4-arg constructors. */
  static class DepsBag {
    TriageDeps base;
    java.util.function.BiFunction<Types.AlertPayload, Types.ApprovalRequest, Types.ApprovalResponse>
        requestHumanApproval;
    FunctionThrowing<String, ShellResult> execShellCommand;
  }

  @FunctionalInterface
  interface FunctionThrowing<T, R> {
    R apply(T t) throws Exception;
  }

  static TriageDeps override(TriageDeps base, java.util.function.Consumer<DepsBag> mutator) {
    DepsBag bag = new DepsBag();
    bag.base = base;
    mutator.accept(bag);
    return new TriageDeps() {
      @Override
      public List<McpToolInfo> mcpListTools(String url) throws Exception {
        return base.mcpListTools(url);
      }

      @Override
      public String mcpCallTool(String url, String n, Map<String, Object> a) throws Exception {
        return base.mcpCallTool(url, n, a);
      }

      @Override
      public Types.ApprovalResponse requestHumanApproval(
          Types.AlertPayload a, Types.ApprovalRequest r) throws Exception {
        return bag.requestHumanApproval != null
            ? bag.requestHumanApproval.apply(a, r)
            : base.requestHumanApproval(a, r);
      }

      @Override
      public ShellResult execShellCommand(String cmd) throws Exception {
        return bag.execShellCommand != null
            ? bag.execShellCommand.apply(cmd)
            : base.execShellCommand(cmd);
      }
    };
  }
}
