

package io.temporal.samples.batch.heartbeatingactivity;

import java.util.Optional;

/**
 * Helper class that is used to iterate over a list of records.
 *
 * <p>A specific implementation depends on a use case. For example, it can execute an SQL DB query
 * or read a comma delimited file. More complex use cases would need passing a different type of
 * offset parameter.
 */
public interface RecordLoader {

  /**
   * Returns the next record.
   *
   * @param offset offset of the next record.
   * @return Record at the offset. Empty optional if offset exceeds the dataset size.
   */
  Optional<SingleRecord> getRecord(int offset);
}
