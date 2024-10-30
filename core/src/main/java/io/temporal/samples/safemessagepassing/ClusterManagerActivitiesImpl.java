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

import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerActivitiesImpl implements ClusterManagerActivities {
  private static final Logger log = LoggerFactory.getLogger(ClusterManagerActivitiesImpl.class);

  @Override
  public void assignNodesToJob(AssignNodesToJobInput input) {
    for (String node : input.getNodes()) {
      log.info("Assigned node " + node + " to job " + input.getJobName());
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void unassignNodesForJob(UnassignNodesForJobInput input) {
    for (String node : input.getNodes()) {
      log.info("Unassigned node " + node + " from job " + input.getJobName());
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<String> findBadNodes(FindBadNodesInput input) {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    Set<String> badNodes =
        input.getNodesToCheck().stream()
            .filter(n -> Integer.parseInt(n) % 5 == 0)
            .collect(Collectors.toSet());
    if (!badNodes.isEmpty()) {
      log.info("Found bad nodes: " + badNodes);
    } else {
      log.info("No bad nodes found");
    }
    return badNodes;
  }

  @Override
  public void shutdown() {
    log.info("Shutting down cluster");
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
