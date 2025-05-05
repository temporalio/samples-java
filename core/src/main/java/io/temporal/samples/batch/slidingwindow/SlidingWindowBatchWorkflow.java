

package io.temporal.samples.batch.slidingwindow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SlidingWindowBatchWorkflow {

  /**
   * Process the batch of records.
   *
   * @return total number of processed records.
   */
  @WorkflowMethod
  int processBatch(ProcessBatchInput input);

  @SignalMethod
  void reportCompletion(int recordId);

  @QueryMethod
  BatchProgress getProgress();
}
