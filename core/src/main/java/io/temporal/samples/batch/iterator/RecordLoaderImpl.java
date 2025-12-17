package io.temporal.samples.batch.iterator;

import java.util.ArrayList;
import java.util.List;

/** Fake implementation of RecordLoader. */
public final class RecordLoaderImpl implements RecordLoader {

  // The sample always returns 5 pages.
  // The real application would iterate over an existing dataset or file.
  public static final int MAX_COUNT = 1000;

  @Override
  public List<SingleRecord> getRecords(int pageSize, int offset) {
    List<SingleRecord> records = new ArrayList<>(pageSize);
    if (offset < MAX_COUNT) {
      for (int i = 0; i < pageSize; i++) {
        records.add(new SingleRecord(offset + i));
      }
    }
    return records;
  }
}
