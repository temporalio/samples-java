package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.workflowstreams.Shared.TickEvent;
import io.temporal.samples.workflowstreams.Shared.TickerInput;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.util.UUID;

/**
 * Scenario 5: bounded log via truncation. The ticker workflow periodically truncates old entries to
 * bound its history, trading complete history for a bounded log. A "fast" subscriber that reads
 * from the start keeps up and sees every tick. A "late" subscriber that joins after truncation and
 * resumes from a stale offset is fast-forwarded to the current base offset — it cannot see the
 * truncated ticks.
 */
public class TruncatingTicker {

  private static final int TICK_COUNT = 30;

  /**
   * keepLast bounds the workflow's log to its most recent entries; truncateEvery controls how often
   * it truncates. They are deliberately small so the early offsets are dropped quickly.
   */
  private static final int KEEP_LAST = 5;

  private static final int TRUNCATE_EVERY = 5;

  /**
   * An early offset the late subscriber deliberately resumes from. By the time it subscribes, the
   * workflow has truncated past it.
   */
  private static final long STALE_OFFSET = 1;

  public static void main(String[] args) throws InterruptedException {
    WorkflowClient client = Shared.newWorkflowClient();

    String workflowId = "workflow-streams-ticker-" + UUID.randomUUID();
    TickerWorkflow workflow =
        client.newWorkflowStub(
            TickerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .build());
    WorkflowClient.start(workflow::tick, new TickerInput(TICK_COUNT, KEEP_LAST, TRUNCATE_EVERY));
    System.out.println("Started workflow: " + workflowId);

    int lastN = TICK_COUNT - 1;

    Thread fast = new Thread(() -> fastSubscriber(client, workflowId, lastN));
    Thread late = new Thread(() -> lateSubscriber(client, workflowId, lastN));
    fast.start();
    late.start();
    fast.join();
    late.join();
    System.exit(0);
  }

  /** Reads from the beginning and keeps up with every tick. */
  private static void fastSubscriber(WorkflowClient client, String workflowId, int lastN) {
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId);
        WorkflowStreamSubscription subscription = stream.topic(Shared.TOPIC_TICK).subscribe(0)) {
      for (WorkflowStreamItem item : subscription) {
        TickEvent evt = Shared.decode(item, TickEvent.class);
        System.out.printf("[fast] offset=%3d  n=%d%n", item.getOffset(), evt.n);
        if (evt.n == lastN) {
          return;
        }
      }
    }
  }

  /**
   * Waits until the workflow has truncated past STALE_OFFSET, then resumes from that
   * (now-truncated) offset. The stream fast-forwards it to the current base offset, so its first
   * item necessarily skips the truncated ticks.
   */
  private static void lateSubscriber(WorkflowClient client, String workflowId, int lastN) {
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId)) {
      // The first truncation only fires once published reaches the first multiple of
      // TRUNCATE_EVERY greater than KEEP_LAST; after it, the base offset is
      // published - KEEP_LAST. Waiting until the head passes that point guarantees the
      // base has advanced beyond STALE_OFFSET.
      int firstTruncate = ((KEEP_LAST / TRUNCATE_EVERY) + 1) * TRUNCATE_EVERY;
      while (stream.getOffset() <= firstTruncate) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }

      try (WorkflowStreamSubscription subscription =
          stream.topic(Shared.TOPIC_TICK).subscribe(STALE_OFFSET)) {
        boolean first = true;
        for (WorkflowStreamItem item : subscription) {
          TickEvent evt = Shared.decode(item, TickEvent.class);
          if (first) {
            if (item.getOffset() > STALE_OFFSET) {
              System.out.printf(
                  "[late] requested offset %d but it was truncated; fast-forwarded to offset %d"
                      + " (skipped %d tick(s))%n",
                  STALE_OFFSET, item.getOffset(), item.getOffset() - STALE_OFFSET);
            }
            first = false;
          }
          System.out.printf("[late] offset=%3d  n=%d%n", item.getOffset(), evt.n);
          if (evt.n == lastN) {
            return;
          }
        }
      }
    }
  }
}
