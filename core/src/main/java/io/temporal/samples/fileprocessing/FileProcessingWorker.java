package io.temporal.samples.fileprocessing;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.lang.management.ManagementFactory;

/**
 * This is the process that hosts all workflows and activities in this sample. Run multiple
 * instances of the worker in different windows. Then start a workflow by running the
 * FileProcessingStarter. Note that all activities always execute on the same worker. But each time
 * they might end up on a different worker as the first activity is dispatched to the common task
 * list.
 */
public class FileProcessingWorker {

  static final String TASK_QUEUE = "FileProcessing";

  public static void main(String[] args) {

    String hostSpecifiTaskQueue = ManagementFactory.getRuntimeMXBean().getName();

    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    final Worker workerForCommonTaskQueue = factory.newWorker(TASK_QUEUE);
    workerForCommonTaskQueue.registerWorkflowImplementationTypes(FileProcessingWorkflowImpl.class);
    StoreActivitiesImpl storeActivityImpl = new StoreActivitiesImpl(hostSpecifiTaskQueue);
    workerForCommonTaskQueue.registerActivitiesImplementations(storeActivityImpl);

    // Get worker to poll the host-specific task queue.
    final Worker workerForHostSpecificTaskQueue = factory.newWorker(hostSpecifiTaskQueue);
    workerForHostSpecificTaskQueue.registerActivitiesImplementations(storeActivityImpl);

    // Start all workers created by this factory.
    factory.start();
    System.out.println("Worker started for task queue: " + TASK_QUEUE);
    System.out.println("Worker Started for activity task Queue: " + hostSpecifiTaskQueue);
  }
}
