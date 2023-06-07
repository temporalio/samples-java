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

/** Simple counter class. */
public class ClientCounter {
  private static final String NUM_OF_GET_RESULT = "numOfGetResult";
  private static final String NUM_OF_WORKFLOW_EXECUTIONS = "numOfWorkflowExec";
  private static final String NUM_OF_SIGNALS = "numOfSignals";
  private static final String NUM_OF_QUERIES = "numOfQueries";
  private static final Map<String, Map<String, Integer>> perWorkflowIdMap =
      Collections.synchronizedMap(new HashMap<>());

  public String getInfo() {
    StringBuilder stringBuilder = new StringBuilder();
    for (String workflowRunId : perWorkflowIdMap.keySet()) {
      stringBuilder.append("\n** Workflow ID: " + workflowRunId);
      Map<String, Integer> info = perWorkflowIdMap.get(workflowRunId);
      stringBuilder.append(
          "\n\tTotal Number of Workflow Exec: " + info.get(NUM_OF_WORKFLOW_EXECUTIONS));
      stringBuilder.append("\n\tTotal Number of Signals: " + info.get(NUM_OF_SIGNALS));
      stringBuilder.append("\n\tTotal Number of Queries: " + info.get(NUM_OF_QUERIES));
      stringBuilder.append("\n\tTotal Number of GetResult: " + info.get(NUM_OF_GET_RESULT));
    }

    return stringBuilder.toString();
  }

  private void add(String workflowId, String type) {
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

  public int getNumOfWorkflowExecutions(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_WORKFLOW_EXECUTIONS);
  }

  public int getNumOfGetResults(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_GET_RESULT);
  }

  public int getNumOfSignals(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_SIGNALS);
  }

  public int getNumOfQueries(String workflowId) {
    return perWorkflowIdMap.get(workflowId).get(NUM_OF_QUERIES);
  }

  /**
   * Creates a default counter info map for a workflowid
   *
   * @return default counter info map
   */
  private Map<String, Integer> getDefaultInfoMap() {
    return Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_WORKFLOW_EXECUTIONS, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_SIGNALS, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_GET_RESULT, 0),
            new AbstractMap.SimpleImmutableEntry<>(NUM_OF_QUERIES, 0))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public void addStartInvocation(String workflowId) {
    add(workflowId, NUM_OF_WORKFLOW_EXECUTIONS);
  }

  public void addSignalInvocation(String workflowId) {
    add(workflowId, NUM_OF_SIGNALS);
  }

  public void addGetResultInvocation(String workflowId) {
    add(workflowId, NUM_OF_GET_RESULT);
  }

  public void addQueryInvocation(String workflowId) {
    add(workflowId, NUM_OF_QUERIES);
  }
}
