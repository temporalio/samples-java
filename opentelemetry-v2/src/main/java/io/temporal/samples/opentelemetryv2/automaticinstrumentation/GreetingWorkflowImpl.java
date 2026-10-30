package io.temporal.samples.opentelemetryv2.automaticinstrumentation;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class GreetingWorkflowImpl implements GreetingWorkflow {
  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String greet(String name) {
    return activities.composeGreeting(name);
  }
}
