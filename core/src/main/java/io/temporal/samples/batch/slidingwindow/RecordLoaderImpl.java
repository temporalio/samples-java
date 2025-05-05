

package io.temporal.samples.batch.slidingwindow;

import java.util.ArrayList;
import java.util.List;

/** Fake loader implementation. The real application would iterate over a dataset or file. */
public final class RecordLoaderImpl implements RecordLoader {

  private static final int TOTAL_COUNT = 300;

  @Override
  public List<SingleRecord> getRecords(int pageSize, int offset) {
    List<SingleRecord> records = new ArrayList<>(pageSize);
    if (offset < TOTAL_COUNT) {
      for (int i = offset; i < Math.min(offset + pageSize, TOTAL_COUNT); i++) {
        records.add(new SingleRecord(i));
      }
    }
    return records;
  }

  @Override
  public int getRecordCount() {
    return TOTAL_COUNT;
  }
}
