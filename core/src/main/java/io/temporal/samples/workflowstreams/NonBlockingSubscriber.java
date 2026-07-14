package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.workflowstreams.Shared.OrderInput;
import io.temporal.samples.workflowstreams.Shared.ProgressEvent;
import io.temporal.samples.workflowstreams.Shared.StatusEvent;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamListener;
import io.temporal.workflowstreams.WorkflowStreamSubscriptionHandle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scenario 2: non-blocking listener. Iterating a {@code WorkflowStreamSubscription} (scenario 1)
 * parks the calling thread between items. The non-blocking alternative registers a {@link
 * WorkflowStreamListener} and gets a {@link WorkflowStreamSubscriptionHandle} back immediately.
 * Here two order workflows are consumed concurrently while every item is rendered on a single
 * shared executor thread; the main thread just waits on the done futures, which complete when each
 * workflow reaches a terminal state. {@code onNext} returns a {@link CompletionStage}, and the
 * subscription delivers the next item only once that stage completes — per-subscription
 * backpressure without a parked thread per subscription.
 */
public class NonBlockingSubscriber {

  public static void main(String[] args) {
    WorkflowClient client = Shared.newWorkflowClient();

    // A single thread renders the items of both subscriptions.
    ExecutorService renderer = Executors.newSingleThreadExecutor();

    String[] orderIds = {"order-A", "order-B"};
    OrderWorkflow[] workflows = new OrderWorkflow[orderIds.length];
    WorkflowStreamClient[] streams = new WorkflowStreamClient[orderIds.length];
    WorkflowStreamSubscriptionHandle[] handles =
        new WorkflowStreamSubscriptionHandle[orderIds.length];

    for (int i = 0; i < orderIds.length; i++) {
      String orderId = orderIds[i];
      String workflowId = "workflow-streams-listener-" + orderId + "-" + UUID.randomUUID();
      workflows[i] =
          client.newWorkflowStub(
              OrderWorkflow.class,
              WorkflowOptions.newBuilder()
                  .setWorkflowId(workflowId)
                  .setTaskQueue(Shared.TASK_QUEUE)
                  .build());
      WorkflowClient.start(workflows[i]::processOrder, new OrderInput(orderId));
      System.out.println("Started workflow: " + workflowId);

      streams[i] = WorkflowStreamClient.newInstance(client, workflowId);
      handles[i] =
          streams[i].subscribe(
              SubscribeOptions.newBuilder()
                  .setTopics(Shared.TOPIC_STATUS, Shared.TOPIC_PROGRESS)
                  .build(),
              new WorkflowStreamListener() {
                @Override
                public CompletionStage<Void> onNext(WorkflowStreamItem item) {
                  // Hand the item off instead of processing it inline. The subscription
                  // waits for the returned stage before delivering the next item.
                  return CompletableFuture.runAsync(() -> render(orderId, item), renderer);
                }

                @Override
                public void onCompleted() {
                  System.out.printf("[%s] stream completed%n", orderId);
                }

                @Override
                public void onError(Throwable t) {
                  System.out.printf("[%s] stream failed: %s%n", orderId, t);
                }
              });
    }

    // Nothing above blocked. Both subscriptions run in the background; wait until each
    // stream drains, then collect the workflow results.
    CompletableFuture.allOf(handles[0].getDoneFuture(), handles[1].getDoneFuture()).join();

    for (int i = 0; i < orderIds.length; i++) {
      handles[i].close();
      streams[i].close();
      String result = WorkflowStub.fromTyped(workflows[i]).getResult(String.class);
      System.out.printf("[%s] workflow result: %s%n", orderIds[i], result);
    }
    renderer.shutdown();
    System.exit(0);
  }

  private static void render(String orderId, WorkflowStreamItem item) {
    if (item.getTopic().equals(Shared.TOPIC_STATUS)) {
      StatusEvent evt = Shared.decode(item, StatusEvent.class);
      System.out.printf("[%s] [status]   %s%n", orderId, evt.kind);
    } else {
      ProgressEvent evt = Shared.decode(item, ProgressEvent.class);
      System.out.printf("[%s] [progress] %s%n", orderId, evt.message);
    }
  }
}
