package io.temporal.samples.resilientfanout;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class SendWelcomeEmailWorkflowImpl implements SendWelcomeEmailWorkflow {

  private final OnboardingActivities activities =
      Workflow.newActivityStub(
          OnboardingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public void sendWelcomeEmail(String userId) {
    activities.sendWelcomeEmail(userId);
  }
}
