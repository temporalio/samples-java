package io.temporal.samples.workerversioning;

import io.temporal.api.workflowservice.v1.DescribeWorkerDeploymentRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkerDeploymentResponse;
import io.temporal.api.workflowservice.v1.SetWorkerDeploymentCurrentVersionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkerDeploymentVersion;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Starter {
  public static final String TASK_QUEUE = "worker-versioning";
  public static final String DEPLOYMENT_NAME = "my-deployment";

  private static final Logger logger = LoggerFactory.getLogger(Starter.class);

  public static void main(String[] args) throws Exception {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Wait for v1 worker and set as current version
    logger.info(
        "Waiting for v1 worker to appear. Run `./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.WorkerV1` in another terminal");
    waitForWorkerAndMakeCurrent(client, service, "1.0");

    // Start auto-upgrading and pinned workflows. Importantly, note that when we start the
    // workflows,
    // we are using a workflow type name which does *not* include the version number. We defined
    // them
    // with versioned names so we could show changes to the code, but here when the client invokes
    // them, we're demonstrating that the client remains version-agnostic.
    String autoUpgradeWorkflowId = "worker-versioning-versioning-autoupgrade_" + UUID.randomUUID();
    WorkflowStub autoUpgradeExecution =
        client.newUntypedWorkflowStub(
            "AutoUpgradingWorkflow",
            WorkflowOptions.newBuilder()
                .setWorkflowId(autoUpgradeWorkflowId)
                .setTaskQueue(TASK_QUEUE)
                .build());

    String pinnedWorkflowId = "worker-versioning-versioning-pinned_" + UUID.randomUUID();
    WorkflowStub pinnedExecution =
        client.newUntypedWorkflowStub(
            "PinnedWorkflow",
            WorkflowOptions.newBuilder()
                .setWorkflowId(pinnedWorkflowId)
                .setTaskQueue(TASK_QUEUE)
                .build());

    // Start workflows asynchronously
    autoUpgradeExecution.start();
    pinnedExecution.start();

    logger.info(
        "Started auto-upgrading workflow: {}", autoUpgradeExecution.getExecution().getWorkflowId());
    logger.info("Started pinned workflow: {}", pinnedExecution.getExecution().getWorkflowId());

    // Signal both workflows a few times to drive them
    advanceWorkflows(autoUpgradeExecution, pinnedExecution);

    // Now wait for the v1.1 worker to appear and become current
    logger.info(
        "Waiting for v1.1 worker to appear. Run `./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.WorkerV1_1` in another terminal");
    waitForWorkerAndMakeCurrent(client, service, "1.1");

    // Once it has, we will continue to advance the workflows.
    // The auto-upgrade workflow will now make progress on the new worker, while the pinned one will
    // keep progressing on the old worker.
    advanceWorkflows(autoUpgradeExecution, pinnedExecution);

    // Finally we'll start the v2 worker, and again it'll become the new current version
    logger.info(
        "Waiting for v2 worker to appear. Run `./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.WorkerV2` in another terminal");
    waitForWorkerAndMakeCurrent(client, service, "2.0");

    // Once it has we'll start one more new workflow, another pinned one, to demonstrate that new
    // pinned workflows start on the current version.
    String pinnedWorkflow2Id = "worker-versioning-versioning-pinned-2_" + UUID.randomUUID();
    WorkflowStub pinnedExecution2 =
        client.newUntypedWorkflowStub(
            "PinnedWorkflow",
            WorkflowOptions.newBuilder()
                .setWorkflowId(pinnedWorkflow2Id)
                .setTaskQueue(TASK_QUEUE)
                .build());
    pinnedExecution2.start();
    logger.info("Started pinned workflow v2: {}", pinnedExecution2.getExecution().getWorkflowId());

    // Now we'll conclude all workflows. You should be able to see in your server UI that the pinned
    // workflow always stayed on 1.0, while the auto-upgrading workflow migrated.
    autoUpgradeExecution.signal("doNextSignal", "conclude");
    pinnedExecution.signal("doNextSignal", "conclude");
    pinnedExecution2.signal("doNextSignal", "conclude");

    // Wait for all workflows to complete
    autoUpgradeExecution.getResult(Void.class);
    pinnedExecution.getResult(Void.class);
    pinnedExecution2.getResult(Void.class);

    logger.info("All workflows completed");
  }

  private static void advanceWorkflows(
      WorkflowStub autoUpgradeExecution, WorkflowStub pinnedExecution) {
    // Signal both workflows a few times to drive them
    for (int i = 0; i < 3; i++) {
      autoUpgradeExecution.signal("doNextSignal", "do-activity");
      pinnedExecution.signal("doNextSignal", "some-signal");
    }
  }

  private static void waitForWorkerAndMakeCurrent(
      WorkflowClient client, WorkflowServiceStubs service, String buildId)
      throws InterruptedException {
    WorkerDeploymentVersion targetVersion = new WorkerDeploymentVersion(DEPLOYMENT_NAME, buildId);

    // Wait for the worker to appear
    while (true) {
      try {
        DescribeWorkerDeploymentRequest describeRequest =
            DescribeWorkerDeploymentRequest.newBuilder()
                .setNamespace(client.getOptions().getNamespace())
                .setDeploymentName(DEPLOYMENT_NAME)
                .build();

        DescribeWorkerDeploymentResponse response =
            service.blockingStub().describeWorkerDeployment(describeRequest);

        // Check if our version is present in the version summaries
        boolean found =
            response.getWorkerDeploymentInfo().getVersionSummariesList().stream()
                .anyMatch(
                    versionSummary ->
                        targetVersion
                                .getDeploymentName()
                                .equals(versionSummary.getDeploymentVersion().getDeploymentName())
                            && targetVersion
                                .getBuildId()
                                .equals(versionSummary.getDeploymentVersion().getBuildId()));

        if (found) {
          break;
        }
      } catch (Exception ignored) {
        // Exception intentionally ignored
      }
      Thread.sleep(1000);
    }

    // Once the version is available, set it as current
    SetWorkerDeploymentCurrentVersionRequest setRequest =
        SetWorkerDeploymentCurrentVersionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setDeploymentName(DEPLOYMENT_NAME)
            .setBuildId(targetVersion.getBuildId())
            .build();

    service.blockingStub().setWorkerDeploymentCurrentVersion(setRequest);
  }
}
