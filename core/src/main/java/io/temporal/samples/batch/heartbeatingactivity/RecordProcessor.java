

package io.temporal.samples.batch.heartbeatingactivity;

/** A helper class that implements record processing. */
public interface RecordProcessor {

  /**
   * Processes a single record.
   *
   * @param record record to process
   */
  void processRecord(SingleRecord record);
}
