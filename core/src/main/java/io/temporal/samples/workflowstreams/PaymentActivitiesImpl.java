package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.ProgressEvent;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamClientOptions;
import java.time.Duration;

public class PaymentActivitiesImpl implements PaymentActivities {

  /**
   * {@code WorkflowStreamClient.fromActivity()} reads the parent workflow id and the Temporal
   * client from the activity context, so this activity can push events back without any wiring.
   * Closing the client flushes any buffered items before the activity returns.
   */
  @Override
  public String chargeCard(String orderId) {
    WorkflowStreamClientOptions options =
        WorkflowStreamClientOptions.newBuilder().setBatchInterval(Duration.ofMillis(200)).build();
    try (WorkflowStreamClient client = WorkflowStreamClient.fromActivity(options)) {
      TopicHandle progress = client.topic(Shared.TOPIC_PROGRESS);
      progress.publish(new ProgressEvent("charging card..."));

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }

      progress.publish(new ProgressEvent("card charged"));
    }
    return "charge-" + orderId;
  }
}
