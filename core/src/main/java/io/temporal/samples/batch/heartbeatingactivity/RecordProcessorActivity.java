

package io.temporal.samples.batch.heartbeatingactivity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RecordProcessorActivity {

  /** Processes all records in the dataset */
  int processRecords();
}
