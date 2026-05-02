package io.temporal.samples.toolregistryincidenttriage;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;

public class Worker {
  public static void main(String[] args) {
    String address = mustEnv("TEMPORAL_ADDRESS");
    String namespace = mustEnv("TEMPORAL_NAMESPACE");
    String apiKey = mustEnv("TEMPORAL_API_KEY");
    String taskQueue = envOr("TEMPORAL_TASK_QUEUE", "triage-java");

    System.out.println(
        "connecting to " + address + " (ns=" + namespace + ") on task queue " + taskQueue);

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(address)
                .addApiKey(() -> apiKey)
                .build());

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    WorkerFactory factory = WorkerFactory.newInstance(client);
    io.temporal.worker.Worker worker = factory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(
        IncidentTriageWorkflowImpl.class, ApprovalWorkflowImpl.class);
    worker.registerActivitiesImplementations(new TriageActivityImpl());

    System.out.println("worker ready — polling " + taskQueue);
    factory.start();
  }

  private static String mustEnv(String name) {
    String v = System.getenv(name);
    if (v == null || v.isEmpty()) {
      System.err.println("missing env var: " + name);
      System.exit(1);
    }
    return v;
  }

  private static String envOr(String name, String def) {
    String v = System.getenv(name);
    return v == null || v.isEmpty() ? def : v;
  }
}
