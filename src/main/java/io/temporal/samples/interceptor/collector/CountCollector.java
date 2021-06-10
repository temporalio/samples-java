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

package io.temporal.samples.interceptor.collector;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.workflow.WorkflowInfo;
import java.util.ArrayList;
import java.util.List;

public class CountCollector {
  private List<WorkflowInfo> workflowInfoList = new ArrayList<>();
  private List<ActivityExecutionContext> activitiesExecutionContextList = new ArrayList<>();
  private List<SignalCollector> signalsInfoList = new ArrayList<>();
  private List<QueriesCollector> queriesInfoList = new ArrayList<>();
  private List<String> executedActivityNamesList = new ArrayList<>();
  private List<String> executedChildWorkflowNamesList = new ArrayList<>();

  public List<WorkflowInfo> getWorkflowInfoList() {
    return workflowInfoList;
  }

  public void setWorkflowInfoList(List<WorkflowInfo> workflowInfoList) {
    this.workflowInfoList = workflowInfoList;
  }

  public List<SignalCollector> getSignalsInfoList() {
    return signalsInfoList;
  }

  public void setSignalsInfoList(List<SignalCollector> signalsInfoList) {
    this.signalsInfoList = signalsInfoList;
  }

  public List<QueriesCollector> getQueriesInfoList() {
    return queriesInfoList;
  }

  public void setQueriesInfoList(List<QueriesCollector> queriesInfoList) {
    this.queriesInfoList = queriesInfoList;
  }

  public List<String> getExecutedActivityNamesList() {
    return executedActivityNamesList;
  }

  public void setExecutedActivityNamesList(List<String> executedActivityNamesList) {
    this.executedActivityNamesList = executedActivityNamesList;
  }

  public List<String> getExecutedChildWorkflowNamesList() {
    return executedChildWorkflowNamesList;
  }

  public void setExecutedChildWorkflowNamesList(List<String> executedChildWorkflowNamesList) {
    this.executedChildWorkflowNamesList = executedChildWorkflowNamesList;
  }

  public List<ActivityExecutionContext> getActivitiesExecutionContextList() {
    return activitiesExecutionContextList;
  }

  public void setActivitiesExecutionContextList(
      List<ActivityExecutionContext> activitiesExecutionContextList) {
    this.activitiesExecutionContextList = activitiesExecutionContextList;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nCounter Information:");
    sb.append("\nNum Workflow executions:" + getWorkflowCount(workflowInfoList));
    sb.append("\nNum Child Workflow Executions: " + getChildWorkflowCount(workflowInfoList));
    sb.append("\nNum Activities executions: " + getActivitiesExecutionContextList().size());
    sb.append("\nNum of Signals: " + signalsInfoList.size());
    sb.append("\nNum of Queries: " + queriesInfoList.size());

    return sb.toString();
  }

  private long getChildWorkflowCount(List<WorkflowInfo> workflowInfoList) {
    return workflowInfoList.stream().filter(wil -> wil.getParentWorkflowId().isPresent()).count();
  }

  private long getWorkflowCount(List<WorkflowInfo> workflowInfoList) {
    return workflowInfoList.stream().filter(wil -> !wil.getParentWorkflowId().isPresent()).count();
  }

  public static class SignalCollector {
    private String signalName;
    private Object[] signalValues;

    public String getSignalName() {
      return signalName;
    }

    public void setSignalName(String signalName) {
      this.signalName = signalName;
    }

    public Object[] getSignalValues() {
      return signalValues;
    }

    public void setSignalValues(Object[] signalValues) {
      this.signalValues = signalValues;
    }
  }

  public static class QueriesCollector {
    private String queryName;
    private Object[] queryValues;

    public String getQueryName() {
      return queryName;
    }

    public void setQueryName(String queryName) {
      this.queryName = queryName;
    }

    public Object[] getQueryValues() {
      return queryValues;
    }

    public void setQueryValues(Object[] queryValues) {
      this.queryValues = queryValues;
    }
  }
}
