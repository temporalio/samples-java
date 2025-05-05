package io.temporal.samples.batch.slidingwindow;

/** Record to process. */
public class SingleRecord {
  private int id;

  public SingleRecord(int id) {
    this.id = id;
  }

  /** Needed for JSON deserialization. */
  public SingleRecord() {}

  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "SingleRecord{" + "id=" + id + '}';
  }
}
