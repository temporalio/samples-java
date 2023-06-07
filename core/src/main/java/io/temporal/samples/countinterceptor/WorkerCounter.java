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

package io.temporal.samples.countinterceptor;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple counter class. Static impl just for the sake of the sample. Note: in your applications you
 * should use CDI for example instead.
 */
public class WorkerCounter {
  private static Map<String, Map<String, Integer>> perWorkflowIdMap =
      Collections.synchronizedMap(new HashMap<>());

  public static final String NUM_OF_WORKFLOW_EXECUTIONS = "numOfWorkflowExec";
  public static final String NUM_OF_CHILD_WORKFLOW_EXECUTIONS = "numOfChildWorkflowExec";
  public static final String NUM_OF_ACTIVITY_EXECUTIONS = "numOfActivityExec";
  public static final String NUM_OF_SIGNALS = "numOfSignals";
  public static final String NUM_OF_QUERIES = "numOfQueries";

  public static void add(String workflowId, String type) {
    if (!perWorkflowIdMap.containsKey(workflowId)) {
      perWorkflowIdMap.put(workflowId, getDefaultInfoMap());
    }

    if (perWorkflowIdMap.get(workflowId).get(type) == null) {
      perWorkflowIdMap.get(workflowId).put(type, 1);
    } else {
      int current = perWorkflowIdMap.get(workflowId).get(type).intValue();
      int next = current + 1;
      perWorkflowIdMap.get(workflowId).put(type, next);
    }
  }

  public static int getNumOfWorkflowExecutions(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_WORKFLOW_EXECUTIONS);
  }

  public static int getNumOfChildWorkflowExecutions(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_CHILD_WORKFLOW_EXECUTIONS);
  }

  public static int getNumOfActivityExecutions(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_ACTIVITY_EXECUTIONS);
  }

  public static int getNumOfSignals(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_SIGNALS);
  }

  public static int getNumOfQueries(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_QUERIES);
  }

  public static String getInfo() {
    StringBuilder stringBuilder = new StringBuilder();
    for (String workflowRunId : perWorkflowIdMap.keySet()) {
      stringBuilder.append("\n** Workflow ID: " + workflowRunId);
      Map<String, Integer> info = perWorkflowIdMap.get(workflowRunId);
      stringBuilder.append(
          "\n\tTotal Number of Workflow Exec: " + info.get(NUM_OF_WORKFLOW_EXECUTIONS));
      stringBuilder.append(
          "\n\tTotal Number of Child Workflow Exec: " + info.get(NUM_OF_CHILD_WORKFLOW_EXECUTIONS));
      stringBuilder.append(
          "\n\tTotal Number of Activity Exec: " + info.get(NUM_OF_ACTIVITY_EXECUTIONS));
      stringBuilder.append("\n\tTotal Number of Signals: " + info.get(NUM_OF_SIGNALS));
      stringBuilder.append("\n\tTotal Number of Queries: " + info.get(NUM_OF_QUERIES));
    }

    return stringBuilder.toString();
  }

  /**
   * Creates a default counter info map for a workflowid
   *
   * @return default counter info map
   */
  private static Map<String, Integer> getDefaultInfoMap() {
    return Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_WORKFLOW_EXECUTIONS, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_CHILD_WORKFLOW_EXECUTIONS, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_ACTIVITY_EXECUTIONS, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_SIGNALS, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_QUERIES, 0))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
