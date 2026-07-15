package io.temporal.samples.temporalmetricsdemo;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.temporalmetricsdemo.workflows.ScenarioWorkflow;
import java.time.Duration;
import java.util.UUID;

public class Starter {

  public static void main(String[] args) throws Exception {
    WorkflowClient client = TemporalConnection.client();

    String name = "Temporal";
    String[] scenarios = {"success", "fail", "timeout", "continue", "cancel"};

    for (String scenario : scenarios) {
      String wid = "scenario-" + scenario + "-" + UUID.randomUUID();

      WorkflowOptions.Builder optionsBuilder =
          WorkflowOptions.newBuilder()
              .setTaskQueue(TemporalConnection.TASK_QUEUE)
              .setWorkflowId(wid);

      // workflow timeout
      if ("timeout".equalsIgnoreCase(scenario)) {
        optionsBuilder.setWorkflowRunTimeout(Duration.ofSeconds(3));
      }

      ScenarioWorkflow wf = client.newWorkflowStub(ScenarioWorkflow.class, optionsBuilder.build());

      System.out.println("\n=== Starting scenario: " + scenario + " workflowId=" + wid + " ===");

      try {
        if ("cancel".equalsIgnoreCase(scenario)) {
          WorkflowClient.start(wf::run, scenario, name);
          Thread.sleep(2000);
          WorkflowStub untyped = client.newUntypedWorkflowStub(wid);
          untyped.cancel();
          untyped.getResult(String.class);
          continue;
        }

        // normal synchronous execution
        String result = wf.run(scenario, name);
        System.out.println("Scenario=" + scenario + " Result=" + result);

      } catch (Exception e) {
        System.out.println(
            "Scenario="
                + scenario
                + " ended: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      }
    }
  }
}
