package io.temporal.samples.temporalcloudopenmetrics;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.temporalcloudopenmetrics.activities.ScenarioActivitiesImpl;
import io.temporal.samples.temporalcloudopenmetrics.workflows.ScenarioWorkflowImpl;
import io.temporal.worker.WorkerFactory;

public class WorkerMain {
  public static void main(String[] args) throws Exception {
    WorkflowClient client = TemporalConnection.client();

    WorkerFactory factory = WorkerFactory.newInstance(client);
    io.temporal.worker.Worker worker = factory.newWorker(TemporalConnection.TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(ScenarioWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ScenarioActivitiesImpl());

    factory.start();
    System.out.println(
        "Worker started. namespace="
            + TemporalConnection.NAMESPACE
            + " taskQueue="
            + TemporalConnection.TASK_QUEUE);

    Thread.sleep(TemporalConnection.WORKER_SECONDS * 1000L);

    System.out.println("Stopping worker after " + TemporalConnection.WORKER_SECONDS + "s");
    factory.shutdown();
  }
}
