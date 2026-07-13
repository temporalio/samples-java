package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/** Worker serving scenarios 1-5. */
public class StreamsWorker {

  public static void main(String[] args) {
    WorkflowClient client = Shared.newWorkflowClient();
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(Shared.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        OrderWorkflowImpl.class,
        PipelineWorkflowImpl.class,
        HubWorkflowImpl.class,
        TickerWorkflowImpl.class);
    worker.registerActivitiesImplementations(new PaymentActivitiesImpl());

    factory.start();
    System.out.println("Worker started for task queue: " + Shared.TASK_QUEUE);
  }
}
