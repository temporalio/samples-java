package io.temporal.samples.workerversioning;

import io.temporal.client.WorkflowClient;
import io.temporal.common.WorkerDeploymentVersion;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerDeploymentOptions;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerV1 {

  private static final Logger logger = LoggerFactory.getLogger(WorkerV1.class);

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

    WorkerDeploymentVersion version = new WorkerDeploymentVersion(Starter.DEPLOYMENT_NAME, "1.0");
    WorkerDeploymentOptions deploymentOptions =
        WorkerDeploymentOptions.newBuilder().setUseVersioning(true).setVersion(version).build();

    WorkerOptions workerOptions =
        WorkerOptions.newBuilder().setDeploymentOptions(deploymentOptions).build();

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(Starter.TASK_QUEUE, workerOptions);

    worker.registerWorkflowImplementationTypes(
        AutoUpgradingWorkflowV1Impl.class, PinnedWorkflowV1Impl.class);
    worker.registerActivitiesImplementations(new ActivitiesImpl());

    logger.info("Starting worker v1 (build 1.0)");
    factory.start();
  }
}
