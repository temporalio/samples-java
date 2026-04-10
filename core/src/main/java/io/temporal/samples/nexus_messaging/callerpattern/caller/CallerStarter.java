package io.temporal.samples.nexus_messaging.callerpattern.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;
import java.util.UUID;

public class CallerStarter {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setNamespace(CallerWorker.NAMESPACE).build());

    CallerWorkflow workflow =
        client.newWorkflowStub(
            CallerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("nexus-messaging-caller-" + UUID.randomUUID())
                .setTaskQueue(CallerWorker.TASK_QUEUE)
                .build());

    List<String> log = workflow.run();
    log.forEach(System.out::println);
  }
}
