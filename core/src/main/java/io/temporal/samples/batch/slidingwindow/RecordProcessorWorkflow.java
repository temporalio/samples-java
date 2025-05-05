

package io.temporal.samples.batch.slidingwindow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/** Workflow that implements processing of a single record. */
@WorkflowInterface
public interface RecordProcessorWorkflow {

  /**
   * Processes a single record. Must report completion to a parent through {@link
   * SlidingWindowBatchWorkflow#reportCompletion(int)}
   */
  @WorkflowMethod
  void processRecord(SingleRecord r);
}
