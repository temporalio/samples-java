

package io.temporal.samples.moneybatch;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class AccountActivityWorker {

  static final String TASK_QUEUE = "Account";

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    Account account = new AccountImpl();
    worker.registerActivitiesImplementations(account);

    factory.start();
    System.out.println("Activity Worker started for task queue: " + TASK_QUEUE);
  }
}
