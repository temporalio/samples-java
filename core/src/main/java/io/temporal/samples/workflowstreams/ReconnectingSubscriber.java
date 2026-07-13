package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.workflowstreams.Shared.PipelineInput;
import io.temporal.samples.workflowstreams.Shared.StageEvent;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.util.UUID;

/**
 * Scenario 3: reconnecting subscriber. A subscriber reads a few events, drops its connection, then
 * a brand-new client resumes from the saved offset without missing events or seeing duplicates —
 * because the events are durable in workflow history, not just held in memory.
 */
public class ReconnectingSubscriber {

  /** How many events the first subscriber reads before disconnecting. */
  private static final int PHASE_1_EVENTS = 2;

  public static void main(String[] args) {
    WorkflowClient client = Shared.newWorkflowClient();

    String workflowId = "workflow-streams-pipeline-" + UUID.randomUUID();
    PipelineWorkflow workflow =
        client.newWorkflowStub(
            PipelineWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .build());
    WorkflowClient.start(workflow::runPipeline, new PipelineInput("pipeline-7"));
    System.out.println("Started workflow: " + workflowId);

    // next is the offset to resume from: one past the last item we consumed.
    long next = 0;

    // Phase 1: connect, read a couple of events, remember our position, disconnect.
    System.out.println("--- phase 1: initial subscriber ---");
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId);
        WorkflowStreamSubscription subscription = stream.topic(Shared.TOPIC_STATUS).subscribe(0)) {
      int seen = 0;
      for (WorkflowStreamItem item : subscription) {
        StageEvent evt = Shared.decode(item, StageEvent.class);
        next = item.getOffset() + 1;
        System.out.printf("offset=%d  stage=%s%n", item.getOffset(), evt.stage);
        seen++;
        if (seen >= PHASE_1_EVENTS) {
          break;
        }
      }
    }

    System.out.printf("--- disconnected; will resume from offset %d ---%n", next);

    // Phase 2: a new client resumes from the saved offset until the pipeline completes.
    System.out.println("--- phase 2: reconnected subscriber ---");
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId);
        WorkflowStreamSubscription subscription =
            stream.topic(Shared.TOPIC_STATUS).subscribe(next)) {
      for (WorkflowStreamItem item : subscription) {
        StageEvent evt = Shared.decode(item, StageEvent.class);
        System.out.printf("offset=%d  stage=%s%n", item.getOffset(), evt.stage);
        if (evt.stage.equals("complete")) {
          break;
        }
      }
    }
    System.exit(0);
  }
}
