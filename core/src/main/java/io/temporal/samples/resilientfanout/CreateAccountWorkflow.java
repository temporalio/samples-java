package io.temporal.samples.resilientfanout;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/** The critical branch: onboarding cannot succeed without an account. */
@WorkflowInterface
public interface CreateAccountWorkflow {

  @WorkflowMethod
  String createAccount(String userId);
}
