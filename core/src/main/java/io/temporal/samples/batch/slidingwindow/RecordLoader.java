

package io.temporal.samples.batch.slidingwindow;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface RecordLoader {

  /**
   * Returns the next page of records.
   *
   * @param offset offset of the next page.
   * @param pageSize maximum number of records to return.
   * @return empty list if no more records to process.
   */
  List<SingleRecord> getRecords(int pageSize, int offset);

  /**
   * Returns the total record count.
   *
   * <p>Used to divide record ranges among partitions. Some applications might choose a completely
   * different approach for partitioning the data set.
   */
  int getRecordCount();
}
