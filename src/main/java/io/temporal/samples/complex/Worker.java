package io.temporal.samples.complex;

import static io.temporal.samples.complex.ComplexWorkflow.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;

public class Worker {
  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder().setTarget("192.168.0.3:7233").build());
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(ComplexWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    // Start listening to the workflow and activity task queues.
    factory.start();
  }
}
