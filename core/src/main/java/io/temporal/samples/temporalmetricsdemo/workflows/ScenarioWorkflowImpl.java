package io.temporal.samples.temporalmetricsdemo.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.temporalmetricsdemo.activities.ScenarioActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ScenarioWorkflowImpl implements ScenarioWorkflow {

  private ScenarioActivities activities() {
    ActivityOptions opts =
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
            .build();

    return Workflow.newActivityStub(ScenarioActivities.class, opts);
  }

  @Override
  public String run(String scenario, String name) {

    // Continue-as-new path
    if ("continue".equalsIgnoreCase(scenario)) {
      Workflow.continueAsNew("success", name);
      return "unreachable - continueAsNew";
    }

    // WORKFLOW timeout path (Starter sets WorkflowRunTimeout to 3s)
    if ("timeout".equalsIgnoreCase(scenario)) {
      Workflow.sleep(Duration.ofSeconds(10)); // exceed workflow run timeout
      return "unreachable - workflow timeout";
    }

    // Everything else uses the same single activity
    return activities().doWork(name, scenario);
  }
}
