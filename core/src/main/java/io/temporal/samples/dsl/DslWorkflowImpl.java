package io.temporal.samples.dsl;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.dsl.model.Flow;
import io.temporal.samples.dsl.model.FlowAction;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DslWorkflowImpl implements DslWorkflow {
  @Override
  public String run(Flow flow, String input) {
    if (flow == null || flow.getActions().isEmpty()) {
      throw ApplicationFailure.newFailure(
          "Flow is null or does not have any actions", "illegal flow");
    }

    try {
      return runActions(flow, input);
    } catch (ActivityFailure e) {
      throw ApplicationFailure.newFailure(
          "failing execution after compensation initiated", e.getCause().getClass().getName());
    }
  }

  private String runActions(Flow flow, String input) {
    List<String> results = new ArrayList<>();
    for (FlowAction action : flow.getActions()) {
      // build activity options based on flow action input
      ActivityOptions.Builder activityOptionsBuilder = ActivityOptions.newBuilder();
      activityOptionsBuilder.setStartToCloseTimeout(
          Duration.ofSeconds(action.getStartToCloseSec()));
      if (action.getRetries() > 0) {
        activityOptionsBuilder.setRetryOptions(
            RetryOptions.newBuilder().setMaximumAttempts(action.getRetries()).build());
      }
      // create untyped activity stub and run activity based on flow action
      ActivityStub activityStub = Workflow.newUntypedActivityStub(activityOptionsBuilder.build());

      results.add(activityStub.execute(action.getAction(), String.class, input));
    }
    return String.join(",", results);
  }
}
