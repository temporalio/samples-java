package io.temporal.samples.resilientfanout;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/** A best-effort branch: onboarding can still succeed, in a degraded state, without this. */
@WorkflowInterface
public interface ProvisionStorageWorkflow {

  @WorkflowMethod
  void provisionStorage(String userId);
}
