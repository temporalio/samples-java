

package io.temporal.samples.earlyreturn;

import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class EarlyReturnWorker {
  private static final String TASK_QUEUE = "EarlyReturnTaskQueue";

  public static void main(String[] args) {
    WorkflowClient client = EarlyReturnClient.setupWorkflowClient();
    startWorker(client);
  }

  private static void startWorker(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(TransactionWorkflowImpl.class);
    worker.registerActivitiesImplementations(new TransactionActivitiesImpl());

    factory.start();
    System.out.println("Worker started");
  }
}
