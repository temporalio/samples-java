

package io.temporal.samples.getresultsasync;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;

public class Worker {
  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  public static final WorkerFactory factory = WorkerFactory.newInstance(client);
  public static final String TASK_QUEUE_NAME = "asyncstartqueue";

  public static void main(String[] args) {
    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    factory.start();
  }
}
