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

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflow.WorkflowLock;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerWorkflowImpl implements ClusterManagerWorkflow {

  private static final Logger logger = LoggerFactory.getLogger(ClusterManagerWorkflowImpl.class);
  private final ClusterManagerState state;
  private final WorkflowLock nodeLock;
  private final Duration sleepInterval;
  private final int maxHistoryLength;

  private ClusterManagerActivities activities =
      Workflow.newActivityStub(
          ClusterManagerActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build(),
          Collections.singletonMap(
              "findBadNodes",
              ActivityOptions.newBuilder()
                  .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                  .build()));

  @WorkflowInit
  public ClusterManagerWorkflowImpl(ClusterManagerInput input) {
    nodeLock = Workflow.newWorkflowLock();
    if (input.getState().isPresent()) {
      state = input.getState().get();
    } else {
      state = new ClusterManagerState();
    }
    if (input.isTestContinueAsNew()) {
      maxHistoryLength = 120;
      sleepInterval = Duration.ofSeconds(1);
    } else {
      sleepInterval = Duration.ofSeconds(10);
      maxHistoryLength = 0;
    }
  }

  @Override
  public ClusterManagerResult run(ClusterManagerInput input) {
    Workflow.await(() -> state.workflowState != ClusterState.NOT_STARTED);
    // The cluster manager is a long-running "entity" workflow so we need to periodically checkpoint
    // its state and
    // continue-as-new.
    while (true) {
      performHealthChecks();
      if (!Workflow.await(
          sleepInterval,
          () -> state.workflowState == ClusterState.SHUTTING_DOWN || shouldContinueAsNew())) {
      } else if (state.workflowState == ClusterState.SHUTTING_DOWN) {
        break;
      } else if (shouldContinueAsNew()) {
        // We don't want to leave any job assignment or deletion handlers half-finished when we
        // continue as new.
        Workflow.await(() -> Workflow.isEveryHandlerFinished());
        logger.info("Continuing as new");
        Workflow.continueAsNew(
            new ClusterManagerInput(Optional.of(state), input.isTestContinueAsNew()));
      }
    }
    // Make sure we finish off handlers such as deleting jobs before we complete the workflow.
    Workflow.await(() -> Workflow.isEveryHandlerFinished());
    return new ClusterManagerResult(getAssignedNodes(null).size(), getBadNodes().size());
  }

  @Override
  public void startCluster() {
    if (state.workflowState != ClusterState.NOT_STARTED) {
      logger.warn("Cannot start cluster in state {}", state.workflowState);
      return;
    }
    state.workflowState = ClusterState.STARTED;
    for (int i = 0; i < 25; i++) {
      state.nodes.put(String.valueOf(i), Optional.empty());
    }
    logger.info("Cluster started");
  }

  @Override
  public boolean stopCluster() {
    if (state.workflowState != ClusterState.STARTED) {
      // This is used as an Update handler so that we can return an error to the caller.
      throw ApplicationFailure.newFailure(
          "Cannot shutdown cluster in state " + state.workflowState, "IllegalState");
    }
    activities.shutdown();
    state.workflowState = ClusterState.SHUTTING_DOWN;
    logger.info("Cluster shut down");
    return true;
  }

  @Override
  public ClusterManagerAssignNodesToJobResult assignNodesToJobs(
      ClusterManagerAssignNodesToJobInput input) {
    Workflow.await(() -> state.workflowState != ClusterState.NOT_STARTED);
    if (state.workflowState == ClusterState.SHUTTING_DOWN) {
      throw ApplicationFailure.newFailure(
          "Cannot assign nodes to a job: Cluster is already shut down", "IllegalState");
    }
    nodeLock.lock();
    try {
      // Idempotency guard.
      if (state.jobAssigned.contains(input.getJobName())) {
        return new ClusterManagerAssignNodesToJobResult(getAssignedNodes(input.getJobName()));
      }
      Set<String> unassignedNodes = getUnassignedNodes();
      if (unassignedNodes.size() < input.getTotalNumNodes()) {
        // If you want the client to receive a failure, either add an update validator and throw the
        // exception from there, or raise an ApplicationFailure. Other exceptions in the main
        // handler will cause the workflow to keep retrying and get it stuck.
        throw ApplicationFailure.newFailure(
            "Cannot assign nodes to a job: Not enough nodes available", "IllegalState");
      }
      Set<String> nodesToAssign =
          unassignedNodes.stream().limit(input.getTotalNumNodes()).collect(Collectors.toSet());
      // This call would be dangerous without nodesLock because it yields control and allows
      // interleaving with deleteJob and performHealthChecks, which both touch this.state.nodes.
      activities.assignNodesToJob(
          new ClusterManagerActivities.AssignNodesToJobInput(nodesToAssign, input.getJobName()));
      for (String node : nodesToAssign) {
        state.nodes.put(node, Optional.of(input.getJobName()));
      }
      state.jobAssigned.add(input.getJobName());
      return new ClusterManagerAssignNodesToJobResult(nodesToAssign);
    } finally {
      nodeLock.unlock();
    }
  }

  @Override
  public void deleteJob(ClusterManagerDeleteJobInput input) {
    Workflow.await(() -> state.workflowState != ClusterState.NOT_STARTED);
    if (state.workflowState == ClusterState.SHUTTING_DOWN) {
      // If you want the client to receive a failure, either add an update validator and throw the
      // exception from there, or raise an ApplicationFailure. Other exceptions in the main handler
      // will cause the workflow to keep retrying and get it stuck.
      throw ApplicationFailure.newFailure(
          "Cannot delete a job: Cluster is already shut down", "IllegalState");
    }
    nodeLock.lock();
    try {
      Set<String> nodesToUnassign = getAssignedNodes(input.getJobName());
      // This call would be dangerous without nodesLock because it yields control and allows
      // interleaving
      // with assignNodesToJob and performHealthChecks, which all touch this.state.nodes.
      activities.unassignNodesForJob(
          new ClusterManagerActivities.UnassignNodesForJobInput(
              nodesToUnassign, input.getJobName()));
      for (String node : nodesToUnassign) {
        state.nodes.put(node, Optional.empty());
      }
    } finally {
      nodeLock.unlock();
    }
  }

  private Set<String> getAssignedNodes(String jobName) {
    if (jobName != null) {
      return state.nodes.entrySet().stream()
          .filter(e -> e.getValue().isPresent() && e.getValue().get().equals(jobName))
          .map(e -> e.getKey())
          .collect(Collectors.toSet());
    } else {
      return state.nodes.entrySet().stream()
          .filter(e -> e.getValue().isPresent() && !e.getValue().get().equals("BAD!"))
          .map(e -> e.getKey())
          .collect(Collectors.toSet());
    }
  }

  private Set<String> getUnassignedNodes() {
    return state.nodes.entrySet().stream()
        .filter(e -> !e.getValue().isPresent())
        .map(e -> e.getKey())
        .collect(Collectors.toSet());
  }

  private Set<String> getBadNodes() {
    return state.nodes.entrySet().stream()
        .filter(e -> e.getValue().isPresent() && e.getValue().get().equals("BAD!"))
        .map(e -> e.getKey())
        .collect(Collectors.toSet());
  }

  private void performHealthChecks() {
    nodeLock.lock();
    try {
      Set<String> assignedNodes = getAssignedNodes(null);
      Set<String> badNodes =
          activities.findBadNodes(new ClusterManagerActivities.FindBadNodesInput(assignedNodes));
      for (String badNode : badNodes) {
        state.nodes.put(badNode, Optional.of("BAD!"));
      }
    } catch (Exception e) {
      logger.error("Health check failed", e);
    } finally {
      nodeLock.unlock();
    }
  }

  private boolean shouldContinueAsNew() {
    if (Workflow.getInfo().isContinueAsNewSuggested()) {
      return true;
    }
    // This is just for ease-of-testing.  In production, we trust temporal to tell us when to
    // continue as new.
    if (maxHistoryLength > 0 && Workflow.getInfo().getHistoryLength() > maxHistoryLength) {
      return true;
    }
    return false;
  }
}
