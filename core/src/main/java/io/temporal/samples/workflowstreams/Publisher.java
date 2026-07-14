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
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.util.UUID;

/**
 * Scenario 1: basic publish/subscribe. Start an order workflow that publishes status events itself
 * and runs an activity that publishes progress events to the same stream, then subscribe to both
 * topics until the order completes.
 */
public class Publisher {

  public static void main(String[] args) {
    WorkflowClient client = Shared.newWorkflowClient();

    String workflowId = "workflow-streams-order-" + UUID.randomUUID();
    OrderWorkflow workflow =
        client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .build());
    WorkflowClient.start(workflow::processOrder, new OrderInput("order-42"));
    System.out.println("Started workflow: " + workflowId);

    // Single subscription over both topics. The loop ends on the in-band "complete"
    // terminator (break) or because the subscription exhausts when the workflow reaches a
    // terminal state without one (e.g. on failure). Either way we then fetch the workflow
    // result, which throws if the workflow failed.
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId);
        WorkflowStreamSubscription subscription =
            stream.subscribe(
                SubscribeOptions.newBuilder()
                    .setTopics(Shared.TOPIC_STATUS, Shared.TOPIC_PROGRESS)
                    .build())) {
      for (WorkflowStreamItem item : subscription) {
        if (item.getTopic().equals(Shared.TOPIC_STATUS)) {
          StatusEvent evt = Shared.decode(item, StatusEvent.class);
          System.out.printf("[status]   %s: order=%s%n", evt.kind, evt.orderId);
          if (evt.kind.equals("complete")) {
            break;
          }
        } else if (item.getTopic().equals(Shared.TOPIC_PROGRESS)) {
          ProgressEvent evt = Shared.decode(item, ProgressEvent.class);
          System.out.printf("[progress] %s%n", evt.message);
        }
      }
    }

    String result = WorkflowStub.fromTyped(workflow).getResult(String.class);
    System.out.println("workflow result: " + result);
    System.exit(0);
  }
}
