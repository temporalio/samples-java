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

import io.temporal.client.*;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ClusterManagerWorkflowWorkerTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(ClusterManagerWorkflowImpl.class)
          .setActivityImplementations(new ClusterManagerActivitiesImpl())
          .build();

  @Test
  public void testSafeMessageHandler() throws ExecutionException, InterruptedException {
    ClusterManagerWorkflow cluster =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                ClusterManagerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    CompletableFuture<ClusterManagerWorkflow.ClusterManagerResult> result =
        WorkflowClient.execute(
            cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));

    cluster.startCluster();

    List<CompletableFuture<ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult>>
        assignJobs = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      assignJobs.add(
          WorkflowStub.fromTyped(cluster)
              .startUpdate(
                  "assignNodesToJobs",
                  WorkflowUpdateStage.ACCEPTED,
                  ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult.class,
                  new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(2, "job-" + i))
              .getResultAsync());
    }
    assignJobs.forEach(
        (f) -> {
          try {
            Assert.assertEquals(2, f.get().getNodesAssigned().size());
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }
        });

    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(1));

    List<CompletableFuture<Void>> deleteJobs = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      deleteJobs.add(
          WorkflowStub.fromTyped(cluster)
              .startUpdate(
                  "deleteJob",
                  WorkflowUpdateStage.ACCEPTED,
                  Void.class,
                  new ClusterManagerWorkflow.ClusterManagerDeleteJobInput("job-" + i))
              .getResultAsync());
    }
    deleteJobs.forEach(CompletableFuture::join);

    cluster.stopCluster();
    Assert.assertEquals(0, result.get().getNumCurrentlyAssignedNodes());
  }

  @Test
  public void testUpdateIdempotency() {
    ClusterManagerWorkflow cluster =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                ClusterManagerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    WorkflowClient.execute(
        cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));

    cluster.startCluster();

    ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult result1 =
        cluster.assignNodesToJobs(
            new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(5, "test-job"));

    ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult result2 =
        cluster.assignNodesToJobs(
            new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(5, "test-job"));

    Assert.assertTrue(result1.getNodesAssigned().size() >= result2.getNodesAssigned().size());
  }

  @Test
  public void testUpdateFailure() throws ExecutionException, InterruptedException {
    ClusterManagerWorkflow cluster =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                ClusterManagerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    CompletableFuture<ClusterManagerWorkflow.ClusterManagerResult> result =
        WorkflowClient.execute(
            cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));

    cluster.startCluster();

    cluster.assignNodesToJobs(
        new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(24, "big-job"));
    WorkflowUpdateException updateFailure =
        Assert.assertThrows(
            WorkflowUpdateException.class,
            () ->
                cluster.assignNodesToJobs(
                    new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(
                        3, "little-job")));
    Assert.assertTrue(updateFailure.getCause() instanceof ApplicationFailure);
    Assert.assertEquals(
        "Cannot assign nodes to a job: Not enough nodes available",
        ((ApplicationFailure) updateFailure.getCause()).getOriginalMessage());

    cluster.stopCluster();
    Assert.assertEquals(
        24, result.get().getNumCurrentlyAssignedNodes() + result.get().getNumBadNodes());
  }
}
