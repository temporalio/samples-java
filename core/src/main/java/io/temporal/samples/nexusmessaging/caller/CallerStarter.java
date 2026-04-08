package io.temporal.samples.nexusmessaging.caller;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexusmessaging.options.ClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerStarter {
  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.DEFAULT_TASK_QUEUE_NAME).build();

    MessageCallerStartHandlerWorkflow messageWorkflow =
        client.newWorkflowStub(MessageCallerStartHandlerWorkflow.class, workflowOptions);

    //    MessageCallerWorkflow messageWorkflow =
    //        client.newWorkflowStub(MessageCallerWorkflow.class, workflowOptions);
    WorkflowExecution execution = WorkflowClient.start(messageWorkflow::sentMessage);
    logger.info(
        "Started readMessage workflowId: {} runId: {}",
        execution.getWorkflowId(),
        execution.getRunId());
    String returnVal = messageWorkflow.sentMessage();
    logger.info("Workflow readMessage done - retval is {}", returnVal);
  }
}
