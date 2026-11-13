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
        SampleConfig.HANDLER_A_1_ENDPOINT + " sync result: " + results.handlerA1SyncResult());
    System.out.println(
        SampleConfig.HANDLER_A_1_ENDPOINT + " async result: " + results.handlerA1AsyncResult());
    System.out.println(
        SampleConfig.HANDLER_B_1_ENDPOINT + " sync result: " + results.handlerB1SyncResult());
    System.out.println(
        SampleConfig.HANDLER_B_1_ENDPOINT + " async result: " + results.handlerB1AsyncResult());
    System.out.println(
        SampleConfig.HANDLER_A_2_ENDPOINT + " sync result: " + results.handlerA2SyncResult());
    System.out.println(
        SampleConfig.HANDLER_A_2_ENDPOINT + " async result: " + results.handlerA2AsyncResult());
    System.out.println(
        SampleConfig.HANDLER_B_2_ENDPOINT + " sync result: " + results.handlerB2SyncResult());
    System.out.println(
        SampleConfig.HANDLER_B_2_ENDPOINT + " async result: " + results.handlerB2AsyncResult());
  }
}
