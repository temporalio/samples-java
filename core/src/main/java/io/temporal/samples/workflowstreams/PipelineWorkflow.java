package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.PipelineInput;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Scenario 2: publishes a sequence of stage events with delays between them, giving a subscriber
 * time to disconnect and reconnect mid-stream.
 */
@WorkflowInterface
public interface PipelineWorkflow {
  @WorkflowMethod
  String runPipeline(PipelineInput input);
}
