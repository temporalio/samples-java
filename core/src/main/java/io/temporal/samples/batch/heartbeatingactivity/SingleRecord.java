package io.temporal.samples.batch.heartbeatingactivity;

/** Record to process. A real application would add a use case specific data. */
public class SingleRecord {
  private int id;

  public SingleRecord(int id) {
    this.id = id;
  }

  /** JSON deserializer needs it */
  public SingleRecord() {}

  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Record{" + "id=" + id + '}';
  }
}
