package io.temporal.samples.batch.heartbeatingactivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fake record processor implementation. */
public final class RecordProcessorImpl implements RecordProcessor {

  private static final Logger log = LoggerFactory.getLogger(RecordProcessorImpl.class);

  @Override
  public void processRecord(SingleRecord record) {
    // Fake processing logic
    try {
      Thread.sleep(100);
      log.info("Processed " + record);
    } catch (InterruptedException ignored) {
      return;
    }
  }
}
