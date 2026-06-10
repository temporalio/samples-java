package io.temporal.samples.lambdaworker;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Local helper that starts a Workflow execution for the Lambda worker to process. */
public class Starter {

  public static void main(String[] args) {
    String name = args.length > 0 ? String.join(" ", args) : "Serverless Lambda Worker!";

    WorkflowServiceStubs service = null;
    try {
      ClientConfigProfile profile = ClientConfigProfile.load();
      service = WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
      WorkflowClient client =
          WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

      SampleWorkflow workflow =
          client.newWorkflowStub(
              SampleWorkflow.class,
              WorkflowOptions.newBuilder()
                  .setWorkflowId(LambdaWorkerSample.workflowIdPrefix() + "-" + UUID.randomUUID())
                  .setTaskQueue(LambdaWorkerSample.taskQueue())
                  .build());

      WorkflowExecution execution = WorkflowClient.start(workflow::getGreeting, name);
      System.out.printf(
          "Started workflow WorkflowID=%s RunID=%s%n",
          execution.getWorkflowId(), execution.getRunId());

      String result = WorkflowStub.fromTyped(workflow).getResult(String.class);
      System.out.println("Workflow result: " + result);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load Temporal client configuration", e);
    } finally {
      if (service != null) {
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }
}
