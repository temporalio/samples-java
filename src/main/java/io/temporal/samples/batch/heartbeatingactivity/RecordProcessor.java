package io.temporal.samples.batch.heartbeatingactivity;

public interface RecordProcessor {

    void processRecord(SingleRecord record);
}
