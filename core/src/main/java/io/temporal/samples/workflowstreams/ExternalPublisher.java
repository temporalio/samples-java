package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.workflowstreams.Shared.HubInput;
import io.temporal.samples.workflowstreams.Shared.NewsEvent;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.util.UUID;

/**
 * Scenario 4: external (non-activity) publisher. The hub workflow does no work of its own; it just
 * hosts the stream. A separate process publishes news into it using the same client factory used to
 * subscribe, then signals the workflow to close. Here the publisher and a subscriber run as two
 * threads.
 */
public class ExternalPublisher {

  private static final String[] HEADLINES = {
    "markets open higher", "new bridge opens downtown", "local team wins championship",
  };

  /** The sentinel the publisher sends last so the subscriber knows to stop. */
  private static final String DONE_HEADLINE = "-- end of feed --";

  public static void main(String[] args) throws InterruptedException {
    WorkflowClient client = Shared.newWorkflowClient();

    String workflowId = "workflow-streams-hub-" + UUID.randomUUID();
    HubWorkflow workflow =
        client.newWorkflowStub(
            HubWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .build());
    WorkflowClient.start(workflow::host, new HubInput("newsroom"));
    System.out.println("Started workflow: " + workflowId);

    Thread subscriber =
        new Thread(
            () -> {
              try (WorkflowStreamClient stream =
                      WorkflowStreamClient.newInstance(client, workflowId);
                  WorkflowStreamSubscription subscription =
                      stream.topic(Shared.TOPIC_NEWS).subscribe(0)) {
                for (WorkflowStreamItem item : subscription) {
                  NewsEvent evt = Shared.decode(item, NewsEvent.class);
                  if (evt.headline.equals(DONE_HEADLINE)) {
                    return;
                  }
                  System.out.printf("[subscriber] %s%n", evt.headline);
                }
              }
            });

    Thread publisher =
        new Thread(
            () -> {
              try (WorkflowStreamClient producer =
                  WorkflowStreamClient.newInstance(client, workflowId)) {
                TopicHandle news = producer.topic(Shared.TOPIC_NEWS);
                for (String headline : HEADLINES) {
                  news.publish(new NewsEvent(headline));
                  System.out.printf("[publisher]  sent: %s%n", headline);
                  try {
                    Thread.sleep(500);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                  }
                }
                // Force-flush the sentinel and wait for the server to confirm delivery
                // before signaling the workflow to close.
                news.publish(new NewsEvent(DONE_HEADLINE), /* forceFlush */ true);
                producer.flush();
              }
              workflow.close();
              System.out.println("[publisher]  signaled close");
            });

    subscriber.start();
    publisher.start();
    subscriber.join();
    publisher.join();
    System.exit(0);
  }
}
