package io.temporal.samples.workerversioning;

import io.temporal.client.WorkflowClient;
import io.temporal.common.WorkerDeploymentVersion;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerDeploymentOptions;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerV1_1 {

  private static final Logger logger = LoggerFactory.getLogger(WorkerV1_1.class);

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerDeploymentVersion version = new WorkerDeploymentVersion(Starter.DEPLOYMENT_NAME, "1.1");
    WorkerDeploymentOptions deploymentOptions =
        WorkerDeploymentOptions.newBuilder().setUseVersioning(true).setVersion(version).build();

    WorkerOptions workerOptions =
        WorkerOptions.newBuilder().setDeploymentOptions(deploymentOptions).build();

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(Starter.TASK_QUEUE, workerOptions);

    worker.registerWorkflowImplementationTypes(
        AutoUpgradingWorkflowV1bImpl.class, PinnedWorkflowV1Impl.class);
    worker.registerActivitiesImplementations(new ActivitiesImpl());

    logger.info("Starting worker v1.1 (build 1.1)");
    factory.start();
  }
}
