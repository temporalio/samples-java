package io.temporal.samples.lambdaworker;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.VersioningBehavior;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowVersioningBehavior;
import java.time.Duration;
import org.slf4j.Logger;

/** Workflow implementation that executes a greeting Activity. */
public class SampleWorkflowImpl implements SampleWorkflow {

  private static final Logger logger = Workflow.getLogger(SampleWorkflowImpl.class);

  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  @WorkflowVersioningBehavior(VersioningBehavior.PINNED)
  public String getGreeting(String name) {
    logger.info("SampleWorkflow started for {}", name);
    String result = activities.createGreeting(name);
    logger.info("SampleWorkflow completed with {}", result);
    return result;
  }
}
