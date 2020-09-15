package io.temporal.samples.helloworld;
// @@@START java-hello-world-sample-workflow-starter
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class HelloWorldStarter {

  public static void main(String args[]) throws Exception {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);
    // Define the name of the Task Queue that the Workflow and Activity Tasks will be sent to
    final String TASK_QUEUE = "java-hello-world-task-queue";
    // Start a workflow execution.
    // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
    HelloWorldWorkflowInterface workflow =
        client.newWorkflowStub(
            HelloWorldWorkflowInterface.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    // Print the greeting to the console
    System.out.println(greeting);
    // Exit the starter program
    System.exit(0);
  }
}
// @@@END
