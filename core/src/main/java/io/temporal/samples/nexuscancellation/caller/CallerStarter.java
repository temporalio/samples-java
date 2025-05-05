package io.temporal.samples.nexuscancellation.caller;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerStarter {
  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.DEFAULT_TASK_QUEUE_NAME).build();
    HelloCallerWorkflow helloWorkflow =
        client.newWorkflowStub(HelloCallerWorkflow.class, workflowOptions);
    WorkflowExecution execution = WorkflowClient.start(helloWorkflow::hello, "Nexus");
    logger.info(
        "Started workflow workflowId: {} runId: {}",
        execution.getWorkflowId(),
        execution.getRunId());
    logger.info("Workflow result: {}", helloWorkflow.hello("Nexus"));
  }
}
