package io.temporal.samples.batch.slidingwindow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Implements BatchWorkflow by running multiple SlidingWindowBatchWorkflows in parallel. */
public class BatchWorkflowImpl implements BatchWorkflow {

  private final RecordLoader recordLoader =
      Workflow.newActivityStub(
          RecordLoader.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public int processBatch(int pageSize, int slidingWindowSize, int partitions) {
    // The sample partitions the data set into continuous ranges.
    // A real application can choose any other way to divide the records into multiple collections.
    int totalCount = recordLoader.getRecordCount();
    int partitionSize = totalCount / partitions + (totalCount % partitions > 0 ? 1 : 0);
    List<Promise<Integer>> results = new ArrayList<>(partitions);
    for (int i = 0; i < partitions; i++) {
      // Makes child id more user-friendly
      String childId = Workflow.getInfo().getWorkflowId() + "/" + i;
      SlidingWindowBatchWorkflow partitionWorkflow =
          Workflow.newChildWorkflowStub(
              SlidingWindowBatchWorkflow.class,
              ChildWorkflowOptions.newBuilder().setWorkflowId(childId).build());
      // Define partition boundaries.
      int offset = partitionSize * i;
      int maximumOffset = Math.min(offset + partitionSize, totalCount);

      ProcessBatchInput input = new ProcessBatchInput();
      input.setPageSize(pageSize);
      input.setSlidingWindowSize(slidingWindowSize);
      input.setOffset(offset);
      input.setMaximumOffset(maximumOffset);

      Promise<Integer> partitionResult = Async.function(partitionWorkflow::processBatch, input);
      results.add(partitionResult);
    }
    int result = 0;
    for (Promise<Integer> partitionResult : results) {
      result += partitionResult.get();
    }
    return result;
  }
}
