package io.temporal.samples.temporalmetricsdemo;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.temporalmetricsdemo.activities.ScenarioActivitiesImpl;
import io.temporal.samples.temporalmetricsdemo.workflows.ScenarioWorkflowImpl;
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
            + TemporalConnection.TASK_QUEUE
            + " metrics=http://0.0.0.0:"
            + System.getenv().getOrDefault("METRICS_PORT", "9464")
            + "/metrics");

    Thread.currentThread().join();
  }
}
