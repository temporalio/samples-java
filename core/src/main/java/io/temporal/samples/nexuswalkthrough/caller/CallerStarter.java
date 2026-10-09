package io.temporal.samples.nexuswalkthrough.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexuswalkthrough.options.ClientOptions;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Starts the caller Workflow, which runs the full approval flow. */
public class CallerStarter {

  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  // @@@SNIPSTART samples-java-nexus-walkthrough-caller-starter
  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.DEFAULT_TASK_QUEUE_NAME).build();

    // Runs the whole flow: context attached first, approval requested, approver reminded, decision
    // submitted, decision awaited, requester notified.
    ApprovalCallerWorkflow workflow = client.newWorkflowStub(ApprovalCallerWorkflow.class, options);
    String result =
        workflow.runApprovalFlow(
            "standing-desk-" + UUID.randomUUID(),
            "dana@example.com",
            1250.00,
            "Approved in the Q3 ergonomics budget");
    logger.info("Purchase result: {}", result);
  }
  // @@@SNIPEND
}
