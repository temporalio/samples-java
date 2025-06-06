package io.temporal.samples.batch.iterator;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements iterator workflow pattern.
 *
 * <p>A single workflow run processes a single page of records in parallel. Each record is processed
 * using its own RecordProcessorWorkflow child workflow.
 *
 * <p>After all child workflows complete the new run of the parent workflow is created using
 * continue as new. The new run processes the next page of records. This way practically unlimited
 * set of records can be processed.
 */
public final class IteratorBatchWorkflowImpl implements IteratorBatchWorkflow {

  private final RecordLoader recordLoader =
      Workflow.newActivityStub(
          RecordLoader.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  /** Stub used to continue-as-new. */
  private final IteratorBatchWorkflow nextRun =
      Workflow.newContinueAsNewStub(IteratorBatchWorkflow.class);

  @Override
  public int processBatch(int pageSize, int offset) {
    // Loads a page of records
    List<SingleRecord> records = recordLoader.getRecords(pageSize, offset);
    // Starts a child per record asynchrnously.
    List<Promise<Void>> results = new ArrayList<>(records.size());
    for (SingleRecord record : records) {
      // Uses human friendly child id.
      String childId = Workflow.getInfo().getWorkflowId() + "/" + record.getId();
      RecordProcessorWorkflow processor =
          Workflow.newChildWorkflowStub(
              RecordProcessorWorkflow.class,
              ChildWorkflowOptions.newBuilder().setWorkflowId(childId).build());
      Promise<Void> result = Async.procedure(processor::processRecord, record);
      results.add(result);
    }
    // Waits for all children to complete.
    Promise.allOf(results).get();

    // Skips error handling for the sample brevity.
    // So failed RecordProcessorWorkflows are ignored.

    // No more records in the dataset. Completes the workflow.
    if (records.isEmpty()) {
      return offset;
    }

    // Continues as new with the increased offset.
    return nextRun.processBatch(pageSize, offset + records.size());
  }
}
