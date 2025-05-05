

package io.temporal.samples.sleepfordays;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;

public class Worker {
  public static final String TASK_QUEUE = "SleepForDaysTaskQueue";
  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  public static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {
    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(SleepForDaysImpl.class);
    worker.registerActivitiesImplementations(new SendEmailActivityImpl());

    factory.start();
    System.out.println("Worker started for task queue: " + TASK_QUEUE);
  }
}
