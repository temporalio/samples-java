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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.*;

/**
 * ClusterManagerWorkflow keeps track of the assignments of a cluster of nodes. Via signals, the
 * cluster can be started and shutdown. Via updates, clients can also assign jobs to nodes and
 * delete jobs. These updates must run atomically.
 */
@WorkflowInterface
public interface ClusterManagerWorkflow {

  enum ClusterState {
    NOT_STARTED,
    STARTED,
    SHUTTING_DOWN
  }

  // In workflows that continue-as-new, it's convenient to store all your state in one serializable
  // structure to make it easier to pass between runs
  class ClusterManagerState {
    public ClusterState workflowState = ClusterState.NOT_STARTED;
    public Map<String, Optional<String>> nodes = new HashMap<>();
    public Set<String> jobAssigned = new HashSet<>();
  }

  class ClusterManagerInput {
    private final Optional<ClusterManagerState> state;
    private final boolean testContinueAsNew;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ClusterManagerInput(
        @JsonProperty("state") Optional<ClusterManagerState> state,
        @JsonProperty("test_continue_as_new") boolean testContinueAsNew) {
      this.state = state;
      this.testContinueAsNew = testContinueAsNew;
    }

    @JsonProperty("state")
    public Optional<ClusterManagerState> getState() {
      return state;
    }

    @JsonProperty("test_continue_as_new")
    public boolean isTestContinueAsNew() {
      return testContinueAsNew;
    }
  }

  class ClusterManagerResult {
    private final int numCurrentlyAssignedNodes;
    private final int numBadNodes;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ClusterManagerResult(
        @JsonProperty("num_currently_assigned_nodes") int numCurrentlyAssignedNodes,
        @JsonProperty("num_bad_nodes") int numBadNodes) {
      this.numCurrentlyAssignedNodes = numCurrentlyAssignedNodes;
      this.numBadNodes = numBadNodes;
    }

    @JsonProperty("num_currently_assigned_nodes")
    public int getNumCurrentlyAssignedNodes() {
      return numCurrentlyAssignedNodes;
    }

    @JsonProperty("num_bad_nodes")
    public int getNumBadNodes() {
      return numBadNodes;
    }
  }

  // Be in the habit of storing message inputs and outputs in serializable structures.
  // This makes it easier to add more overtime in a backward-compatible way.
  class ClusterManagerAssignNodesToJobInput {
    // If larger or smaller than previous amounts, will resize the job.
    private final int totalNumNodes;
    private final String jobName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ClusterManagerAssignNodesToJobInput(
        @JsonProperty("total_num_nodes") int totalNumNodes,
        @JsonProperty("job_name") String jobName) {
      this.totalNumNodes = totalNumNodes;
      this.jobName = jobName;
    }

    @JsonProperty("total_num_nodes")
    public int getTotalNumNodes() {
      return totalNumNodes;
    }

    @JsonProperty("job_name")
    public String getJobName() {
      return jobName;
    }
  }

  class ClusterManagerDeleteJobInput {
    private final String jobName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ClusterManagerDeleteJobInput(@JsonProperty("job_name") String jobName) {
      this.jobName = jobName;
    }

    @JsonProperty("job_name")
    public String getJobName() {
      return jobName;
    }
  }

  class ClusterManagerAssignNodesToJobResult {
    private final Set<String> nodesAssigned;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ClusterManagerAssignNodesToJobResult(
        @JsonProperty("assigned_nodes") Set<String> assignedNodes) {
      this.nodesAssigned = assignedNodes;
    }

    @JsonProperty("assigned_nodes")
    public Set<String> getNodesAssigned() {
      return nodesAssigned;
    }
  }

  @WorkflowMethod
  ClusterManagerResult run(ClusterManagerInput input);

  @SignalMethod
  void startCluster();

  @UpdateMethod
  boolean stopCluster();

  // This is an update as opposed to a signal because the client may want to wait for nodes to be
  // allocated before sending work to those nodes. Returns the list of node names that were
  // allocated to the job.
  @UpdateMethod
  ClusterManagerAssignNodesToJobResult assignNodesToJobs(ClusterManagerAssignNodesToJobInput input);

  // Even though it returns nothing, this is an update because the client may want to track it, for
  // example to wait for nodes to be unassigned before reassigning them.
  @UpdateMethod
  void deleteJob(ClusterManagerDeleteJobInput input);
}
