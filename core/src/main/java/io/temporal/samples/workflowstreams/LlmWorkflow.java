package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.LlmInput;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Scenario 6: hosts the stream while a streaming activity owns the non-deterministic OpenAI call
 * and publishes token deltas back to subscribers.
 */
@WorkflowInterface
public interface LlmWorkflow {
  @WorkflowMethod
  String complete(LlmInput input);
}
