package io.temporal.samples.nexusserializationcontext.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexusserializationcontext.SampleConfig;

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
        SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT
            + " sync result: "
            + results.compressedEncrypted1SyncResult());
    System.out.println(
        SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT
            + " async result: "
            + results.compressedEncrypted1AsyncResult());
    System.out.println(
        SampleConfig.ENCRYPTED_1_ENDPOINT + " sync result: " + results.encrypted1SyncResult());
    System.out.println(
        SampleConfig.ENCRYPTED_1_ENDPOINT + " async result: " + results.encrypted1AsyncResult());
    System.out.println(
        SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT
            + " sync result: "
            + results.compressedEncrypted2SyncResult());
    System.out.println(
        SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT
            + " async result: "
            + results.compressedEncrypted2AsyncResult());
    System.out.println(
        SampleConfig.ENCRYPTED_2_ENDPOINT + " sync result: " + results.encrypted2SyncResult());
    System.out.println(
        SampleConfig.ENCRYPTED_2_ENDPOINT + " async result: " + results.encrypted2AsyncResult());
  }
}
