package io.temporal.samples.opentelemetryv2.logs;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

public class GreetingWorkflowImpl implements GreetingWorkflow {
  private static final Logger logger = Workflow.getLogger(GreetingWorkflowImpl.class);

  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String greet(String name) {
    logger.info("Requesting greeting for {}", name);
    return activities.composeGreeting(name);
  }
}
