

package io.temporal.samples.batch.slidingwindow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface BatchWorkflow {

  /**
   * Processes a batch of records using multiple parallel sliding window workflows.
   *
   * @param pageSize the number of records to start processing in a single sliding window workflow
   *     run.
   * @param slidingWindowSize the number of records to process in parallel by a single sliding
   *     window workflow. Can be larger than the pageSize.
   * @param partitions defines the number of SlidingWindowBatchWorkflows to run in parallel. If
   *     number of partitions is too low the update rate of a single SlidingWindowBatchWorkflows can
   *     get too high.
   * @return total number of processed records.
   */
  @WorkflowMethod
  int processBatch(int pageSize, int slidingWindowSize, int partitions);
}
