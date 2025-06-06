package io.temporal.samples.polling.periodicsequence;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.samples.polling.TestService;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class PeriodicPollingStarter {
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final String taskQueue = "pollingSampleQueue";
  private static final String workflowId = "PeriodicPollingSampleWorkflow";

  public static void main(String[] args) {
    // Create our worker and register workflow and activities
    createWorker();

    // Create typed workflow stub and start execution (sync, wait for results)
    PollingWorkflow workflow =
        client.newWorkflowStub(
            PollingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(taskQueue).setWorkflowId(workflowId).build());
    String result = workflow.exec();
    System.out.println("Result: " + result);
    System.exit(0);
  }

  private static void createWorker() {
    WorkerFactory workerFactory = WorkerFactory.newInstance(client);
    Worker worker = workerFactory.newWorker(taskQueue);

    // Register workflow and activities
    worker.registerWorkflowImplementationTypes(
        PeriodicPollingWorkflowImpl.class, PeriodicPollingChildWorkflowImpl.class);
    worker.registerActivitiesImplementations(new PeriodicPollingActivityImpl(new TestService(50)));

    workerFactory.start();
  }
}
