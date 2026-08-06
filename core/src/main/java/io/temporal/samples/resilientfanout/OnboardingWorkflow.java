package io.temporal.samples.resilientfanout;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OnboardingWorkflow {

  @WorkflowMethod
  OnboardingResult onboard(String userId);
}
