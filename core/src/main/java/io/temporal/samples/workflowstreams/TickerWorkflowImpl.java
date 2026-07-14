package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.TickEvent;
import io.temporal.samples.workflowstreams.Shared.TickerInput;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowTopicHandle;
import java.time.Duration;

public class TickerWorkflowImpl implements TickerWorkflow {

  private final WorkflowStream stream;
  private final WorkflowTopicHandle tick;

  @WorkflowInit
  public TickerWorkflowImpl(TickerInput input) {
    stream = WorkflowStream.newInstance(input.streamState);
    tick = stream.topic(Shared.TOPIC_TICK);
  }

  @Override
  public String tick(TickerInput input) {
    int count = input.count != 0 ? input.count : 50;
    int keepLast = input.keepLast != 0 ? input.keepLast : 10;
    int truncateEvery = input.truncateEvery != 0 ? input.truncateEvery : 5;
    long intervalMs = input.intervalMs != 0 ? input.intervalMs : 200;

    int published = 0;
    for (int n = 0; n < count; n++) {
      tick.publish(new TickEvent(n));
      published++;
      Workflow.sleep(Duration.ofMillis(intervalMs));

      if (published % truncateEvery == 0 && published > keepLast) {
        // Drop everything except the last keepLast entries. Future polls positioned
        // before the new base offset are fast-forwarded.
        stream.truncate(published - keepLast);
      }
    }

    // The final tick (n == count - 1) is the in-band terminator subscribers break on.
    // keepLast guarantees that final offset survives the last truncation so even slow
    // consumers eventually see it. Hold the run open briefly so the final poll delivers it.
    Workflow.sleep(OrderWorkflowImpl.DRAIN_DELAY);
    return "ticker emitted " + published + " events";
  }
}
