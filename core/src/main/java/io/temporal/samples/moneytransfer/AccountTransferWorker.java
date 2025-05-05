

package io.temporal.samples.moneytransfer;

import static io.temporal.samples.moneytransfer.AccountActivityWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class AccountTransferWorker {

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    // Get worker to poll the common task queue.
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(AccountTransferWorkflowImpl.class);
    // Start all workers created by this factory.
    factory.start();
    System.out.println("Worker started for task queue: " + TASK_QUEUE);
  }
}
