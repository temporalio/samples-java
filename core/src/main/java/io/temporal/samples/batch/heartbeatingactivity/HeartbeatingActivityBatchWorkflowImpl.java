package io.temporal.samples.batch.heartbeatingactivity;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * A sample implementation of processing a batch by an activity.
 *
 * <p>An activity can run as long as needed. It reports that it is still alive through heartbeat. If
 * the worker is restarted the activity is retried after the heartbeat timeout. Temporal allows
 * store data in heartbeat _details_. These details are available to the next activity attempt. The
 * progress of the record processing is stored in the details to avoid reprocessing records from the
 * beginning on failures.
 */
public final class HeartbeatingActivityBatchWorkflowImpl
    implements HeartbeatingActivityBatchWorkflow {

  /**
   * Activity that is used to process batch records. The start-to-close timeout is set to a high
   * value to support large batch sizes. Heartbeat timeout is required to quickly restart the
   * activity in case of failures. The heartbeat timeout is also needed to record heartbeat details
   * at the service.
   */
  private final RecordProcessorActivity recordProcessor =
      Workflow.newActivityStub(
          RecordProcessorActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofHours(1))
              .setHeartbeatTimeout(Duration.ofSeconds(10))
              .build());

  @Override
  public int processBatch() {
    // No special logic needed here as activity is retried automatically by the service.
    return recordProcessor.processRecords();
  }
}
