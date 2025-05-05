

package io.temporal.samples.safemessagepassing;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerWorkflowWorker {
  private static final Logger logger = LoggerFactory.getLogger(ClusterManagerWorkflowWorker.class);
  static final String TASK_QUEUE = "ClusterManagerWorkflowTaskQueue";
  static final String CLUSTER_MANAGER_WORKFLOW_ID = "ClusterManagerWorkflow";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    final Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ClusterManagerWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ClusterManagerActivitiesImpl());
    factory.start();
    logger.info("Worker started for task queue: " + TASK_QUEUE);
  }
}
