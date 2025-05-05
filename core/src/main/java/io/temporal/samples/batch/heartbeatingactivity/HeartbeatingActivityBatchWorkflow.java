package io.temporal.samples.batch.heartbeatingactivity;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HeartbeatingActivityBatchWorkflow {

  /**
   * Processes the batch of records.
   *
   * @return total number of processed records.
   */
  @WorkflowMethod
  int processBatch();
}
