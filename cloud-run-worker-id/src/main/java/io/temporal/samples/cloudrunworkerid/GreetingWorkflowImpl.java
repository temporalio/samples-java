package io.temporal.samples.cloudrunworkerid;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.VersioningBehavior;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowVersioningBehavior;
import java.time.Duration;

/**
 * Greeting workflow implementation.
 *
 * <p>The method is annotated {@link VersioningBehavior#PINNED}, matching the PINNED default that
 * {@link io.temporal.gcp.cloudrun.GoogleCloudRunMetadata} applies to the worker, so executions stay
 * on the Cloud Run revision that started them.
 */
public final class GreetingWorkflowImpl implements GreetingWorkflow {

  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  @WorkflowVersioningBehavior(VersioningBehavior.PINNED)
  public String getGreeting(String name) {
    return activities.composeGreeting(name);
  }
}
