package io.temporal.samples.workflowstreams;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.workflowstreams.Shared.OrderInput;
import io.temporal.samples.workflowstreams.Shared.ProgressEvent;
import io.temporal.samples.workflowstreams.Shared.StatusEvent;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowTopicHandle;
import java.time.Duration;

public class OrderWorkflowImpl implements OrderWorkflow {

  /**
   * Gives subscribers a moment to poll for the final published items before the workflow completes
   * and the stream stops serving polls.
   */
  static final Duration DRAIN_DELAY = Duration.ofMillis(500);

  private final WorkflowStream stream;
  private final WorkflowTopicHandle status;
  private final WorkflowTopicHandle progress;

  private final PaymentActivities activities =
      Workflow.newActivityStub(
          PaymentActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(1)).build());

  /**
   * Construct the stream from a {@code @WorkflowInit} constructor so its handlers are registered
   * before the workflow accepts any messages. Threading {@code streamState} lets the workflow
   * survive continue-as-new without losing buffered items.
   */
  @WorkflowInit
  public OrderWorkflowImpl(OrderInput input) {
    stream = WorkflowStream.newInstance(input.streamState);
    status = stream.topic(Shared.TOPIC_STATUS);
    progress = stream.topic(Shared.TOPIC_PROGRESS);
  }

  @Override
  public String processOrder(OrderInput input) {
    status.publish(new StatusEvent("received", input.orderId));

    String chargeId = activities.chargeCard(input.orderId);

    status.publish(new StatusEvent("shipped", input.orderId));
    progress.publish(new ProgressEvent("charge id: " + chargeId));
    status.publish(new StatusEvent("complete", input.orderId));

    // The "complete" status event above is the in-band terminator subscribers break on
    // (see Publisher). Hold the run open briefly so subscribers' next poll delivers it
    // before this run completes and the in-memory log is gone.
    Workflow.sleep(DRAIN_DELAY);
    return chargeId;
  }
}
