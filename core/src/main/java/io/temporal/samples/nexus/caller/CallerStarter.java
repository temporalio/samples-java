package io.temporal.samples.nexus.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexus.service.NexusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerStarter {
  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.DEFAULT_TASK_QUEUE_NAME).build();
    EchoCallerWorkflow echoWorkflow =
        client.newWorkflowStub(EchoCallerWorkflow.class, workflowOptions);
    logger.info("Workflow result: " + echoWorkflow.echo("Nexus Echo 👋"));
    HelloCallerWorkflow helloWorkflow =
        client.newWorkflowStub(HelloCallerWorkflow.class, workflowOptions);
    logger.info("Workflow result: " + helloWorkflow.hello("Nexus", NexusService.Language.ES));
  }
}
