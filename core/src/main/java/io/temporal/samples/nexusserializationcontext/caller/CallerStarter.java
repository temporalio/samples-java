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
            WorkflowClientOptions.newBuilder().setDataConverter(SampleConfig.dataConverter()));
    CallerWorkflow workflow =
        client.newWorkflowStub(
            CallerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(SampleConfig.CALLER_TASK_QUEUE).build());

    EndpointResults results = workflow.echoThroughBothEndpoints("Hello from Nexus");
    System.out.println(
        "Compressed and encrypted endpoint result: " + results.compressedEncryptedResult());
    System.out.println("Encrypted endpoint result: " + results.encryptedResult());
  }
}
