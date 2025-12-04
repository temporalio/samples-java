package io.temporal.samples.safemessagepassing;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerWorkflowWorker {
  private static final Logger logger = LoggerFactory.getLogger(ClusterManagerWorkflowWorker.class);
  static final String TASK_QUEUE = "ClusterManagerWorkflowTaskQueue";
  static final String CLUSTER_MANAGER_WORKFLOW_ID = "ClusterManagerWorkflow";

  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
    WorkerFactory factory = WorkerFactory.newInstance(client);
    final Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ClusterManagerWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ClusterManagerActivitiesImpl());
    factory.start();
    logger.info("Worker started for task queue: " + TASK_QUEUE);
  }
}
