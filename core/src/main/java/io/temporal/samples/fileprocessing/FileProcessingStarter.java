package io.temporal.samples.fileprocessing;

import static io.temporal.samples.fileprocessing.FileProcessingWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.net.URL;

/** Starts a file processing sample workflow. */
public class FileProcessingStarter {

  public static void main(String[] args) throws Exception {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    // gRPC stubs wrapper that talks to the temporal service.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
    FileProcessingWorkflow workflow =
        client.newWorkflowStub(
            FileProcessingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    System.out.println("Executing FileProcessingWorkflow");

    URL source = new URL("http://www.google.com/");
    URL destination = new URL("http://dummy");

    // This is going to block until the workflow completes.
    // This is rarely used in production. Use the commented code below for async start version.
    workflow.processFile(source, destination);
    System.out.println("FileProcessingWorkflow completed");

    // Use this code instead of the above blocking call to start workflow asynchronously.
    //    WorkflowExecution workflowExecution =
    //        WorkflowClient.start(workflow::processFile, source, destination);
    //    System.out.println(
    //        "Started periodic workflow with workflowId=\""
    //            + workflowExecution.getWorkflowId()
    //            + "\" and runId=\""
    //            + workflowExecution.getRunId()
    //            + "\"");
    //
    System.exit(0);
  }
}
