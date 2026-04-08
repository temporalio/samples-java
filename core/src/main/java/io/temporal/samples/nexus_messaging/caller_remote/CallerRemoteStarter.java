package io.temporal.samples.nexus_messaging.caller_remote;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;
import java.util.UUID;

public class CallerRemoteStarter {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setNamespace(CallerRemoteWorker.NAMESPACE).build());

    CallerRemoteWorkflow workflow =
        client.newWorkflowStub(
            CallerRemoteWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("nexus-messaging-remote-caller-" + UUID.randomUUID())
                .setTaskQueue(CallerRemoteWorker.TASK_QUEUE)
                .build());

    List<String> log = workflow.run();
    log.forEach(System.out::println);
  }
}
