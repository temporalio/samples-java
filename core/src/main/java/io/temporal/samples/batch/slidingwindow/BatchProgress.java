

package io.temporal.samples.batch.slidingwindow;

import java.util.Set;

/** Used as a result of {@link SlidingWindowBatchWorkflow#getProgress()} query. */
public final class BatchProgress {

  private final int progress;

  private final Set<Integer> currentRecords;

  public BatchProgress(int progress, Set<Integer> currentRecords) {
    this.progress = progress;
    this.currentRecords = currentRecords;
  }

  /** Count of completed record processing child workflows. */
  public int getProgress() {
    return progress;
  }

  /** Ids of records that are currently being processed by child workflows. */
  public Set<Integer> getCurrentRecords() {
    return currentRecords;
  }
}
