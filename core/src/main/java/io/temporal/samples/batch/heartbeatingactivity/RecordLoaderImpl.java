package io.temporal.samples.batch.heartbeatingactivity;

import java.util.Optional;

/** Fake implementation of RecordLoader. */
public final class RecordLoaderImpl implements RecordLoader {

  static final int RECORD_COUNT = 1000;

  @Override
  public Optional<SingleRecord> getRecord(int offset) {
    if (offset >= RECORD_COUNT) {
      return Optional.empty();
    }
    return Optional.of(new SingleRecord(offset));
  }
}
