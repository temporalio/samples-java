/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.safemessagepassing;

import static io.temporal.samples.safemessagepassing.ClusterManagerWorkflowWorker.CLUSTER_MANAGER_WORKFLOW_ID;
import static io.temporal.samples.safemessagepassing.ClusterManagerWorkflowWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerWorkflowStarter {

  private static final Logger logger = LoggerFactory.getLogger(ClusterManagerWorkflowStarter.class);

  public static void main(String[] args) {
    if (args.length > 1) {
      System.err.println(
          "Usage: java "
              + ClusterManagerWorkflowStarter.class.getName()
              + " <test continue as new>");
      System.exit(1);
    }
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    boolean shouldTestContinueAsNew = args.length > 0 ? Boolean.parseBoolean(args[0]) : false;
    ClusterManagerWorkflow cluster =
        client.newWorkflowStub(
            ClusterManagerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(CLUSTER_MANAGER_WORKFLOW_ID + "-" + UUID.randomUUID())
                .build());

    logger.info("Starting cluster");
    WorkflowClient.start(
        cluster::run,
        new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), shouldTestContinueAsNew));
    Duration delay = shouldTestContinueAsNew ? Duration.ofSeconds(10) : Duration.ofSeconds(1);
    cluster.startCluster();
    logger.info("Assigning jobs to nodes...");
    List<CompletableFuture<ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult>>
        assignJobs = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      assignJobs.add(
          WorkflowStub.fromTyped(cluster)
              .startUpdate(
                  "assignNodesToJobs",
                  WorkflowUpdateStage.ACCEPTED,
                  ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult.class,
                  new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(2, "job" + i))
              .getResultAsync());
    }
    assignJobs.forEach(CompletableFuture::join);

    logger.info("Sleeping for " + delay.getSeconds() + " seconds");
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    logger.info("Deleting jobs...");
    List<CompletableFuture<Void>> deleteJobs = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      deleteJobs.add(
          WorkflowStub.fromTyped(cluster)
              .startUpdate(
                  "deleteJob",
                  WorkflowUpdateStage.ACCEPTED,
                  Void.class,
                  new ClusterManagerWorkflow.ClusterManagerDeleteJobInput("job" + i))
              .getResultAsync());
    }
    deleteJobs.forEach(CompletableFuture::join);

    logger.info("Stopping cluster...");
    cluster.stopCluster();

    ClusterManagerWorkflow.ClusterManagerResult result =
        cluster.run(new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));
    logger.info(
        "Cluster shut down successfully.  It had "
            + result.getNumCurrentlyAssignedNodes()
            + " nodes assigned at the end.");
    System.exit(0);
  }
}
