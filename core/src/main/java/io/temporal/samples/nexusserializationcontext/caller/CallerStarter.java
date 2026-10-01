package io.temporal.samples.nexusserializationcontext.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.options.ClientOptions;

public class CallerStarter {
  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder().setDataConverter(CallerWorker.dataConverter()));
    CallerWorkflow workflow =
        client.newWorkflowStub(
            CallerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.TASK_QUEUE).build());

    EndpointResults results = workflow.echoThroughEndpoints("Hello from Nexus");
    System.out.println(
        "Compressed and encrypted endpoint sync result: "
            + results.compressedEncryptedSyncResult());
    System.out.println("Encrypted endpoint sync result: " + results.encryptedSyncResult());
    System.out.println("Async encrypted endpoint result: " + results.asyncEncryptedResult());
    System.out.println(
        "Async compressed and encrypted endpoint result: "
            + results.asyncCompressedEncryptedResult());
  }
}
