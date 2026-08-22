package io.temporal.samples.nexuswalkthrough.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexuswalkthrough.options.ClientOptions;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the caller Workflow twice, to show both branches of the flow.
 *
 * <p>The first purchase is under the spend threshold, so checkApprovalRequired answers "no" and the
 * caller stops there without creating anything durable. The second is over the threshold and runs
 * the full approval.
 */
public class CallerStarter {

  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  // @@@SNIPSTART samples-java-nexus-walkthrough-caller-starter
  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.DEFAULT_TASK_QUEUE_NAME).build();

    // A small purchase. checkApprovalRequired returns false and nothing durable is created.
    ApprovalCallerWorkflow small = client.newWorkflowStub(ApprovalCallerWorkflow.class, options);
    String smallResult =
        small.runApprovalFlow(
            "laptop-charger-" + UUID.randomUUID(),
            "dana@example.com",
            49.99,
            "Replacement charger");
    logger.info("Small purchase result: {}", smallResult);

    // A large purchase. Runs the whole flow: context attached first, approval requested, approver
    // reminded, decision submitted, decision awaited, requester notified.
    ApprovalCallerWorkflow large = client.newWorkflowStub(ApprovalCallerWorkflow.class, options);
    String largeResult =
        large.runApprovalFlow(
            "standing-desk-" + UUID.randomUUID(),
            "dana@example.com",
            1250.00,
            "Approved in the Q3 ergonomics budget");
    logger.info("Large purchase result: {}", largeResult);
  }
  // @@@SNIPEND
}
