package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.workflowstreams.Shared.LlmInput;
import io.temporal.samples.workflowstreams.Shared.RetryEvent;
import io.temporal.samples.workflowstreams.Shared.TextDelta;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.util.UUID;

/**
 * Scenario 6: LLM token streaming. The workflow hosts the stream while an activity makes the
 * streaming OpenAI call and republishes each token delta. On a retry the activity emits a
 * RetryEvent and this subscriber rewinds the terminal and re-renders. Run {@link LlmWorker} with
 * {@code OPENAI_API_KEY} set before running this.
 */
public class Llm {

  /**
   * ANSI escapes to save the cursor position and to restore it while clearing everything below, so
   * a retry can re-render the completion from scratch.
   */
  private static final String ANSI_SAVE = "\u001b[s";

  private static final String ANSI_RESTORE_AND_CLEAR = "\u001b[u\u001b[J";

  public static void main(String[] args) {
    String prompt = args.length > 0 ? args[0] : "In one short paragraph, explain what Temporal is.";

    WorkflowClient client = Shared.newWorkflowClient();

    String workflowId = "workflow-streams-llm-" + UUID.randomUUID();
    LlmWorkflow workflow =
        client.newWorkflowStub(
            LlmWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Shared.LLM_TASK_QUEUE)
                .build());
    WorkflowClient.start(workflow::complete, new LlmInput(prompt, null));
    System.out.println("Started workflow: " + workflowId);

    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId);
        WorkflowStreamSubscription subscription =
            stream.subscribe(
                SubscribeOptions.newBuilder()
                    .setTopics(Shared.TOPIC_DELTA, Shared.TOPIC_RETRY, Shared.TOPIC_COMPLETE)
                    .build())) {
      System.out.print(ANSI_SAVE);
      for (WorkflowStreamItem item : subscription) {
        if (item.getTopic().equals(Shared.TOPIC_RETRY)) {
          RetryEvent evt = Shared.decode(item, RetryEvent.class);
          System.out.print(ANSI_RESTORE_AND_CLEAR);
          System.out.printf("[retry attempt %d] resetting output%n%n", evt.attempt);
          System.out.print(ANSI_SAVE);
        } else if (item.getTopic().equals(Shared.TOPIC_DELTA)) {
          TextDelta evt = Shared.decode(item, TextDelta.class);
          System.out.print(evt.text);
          System.out.flush();
        } else if (item.getTopic().equals(Shared.TOPIC_COMPLETE)) {
          System.out.println();
          break;
        }
      }
    }
    System.exit(0);
  }
}
