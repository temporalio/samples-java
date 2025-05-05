

package io.temporal.samples.batch.iterator;

import io.temporal.activity.ActivityInterface;
import java.util.List;

/**
 * Activity that is used to iterate over a list of records.
 *
 * <p>A specific implementation depends on a use case. For example, it can execute an SQL DB query
 * or read a comma delimited file. More complex use cases would need passing a different type of
 * offset parameter.
 */
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
}
