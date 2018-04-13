/*
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

package com.uber.cadence.samples.common;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.common.WorkflowExecutionUtils;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;

/**
 * Prints a workflow execution history to the console.
 *
 * @author fateev
 */
public class WorkflowExecutionHistoryPrinter {

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println(
          "Usage: java "
              + WorkflowExecutionHistoryPrinter.class.getName()
              + " <workflowId> <runId>");
      System.exit(1);
    }
    IWorkflowService cadenceService = new WorkflowServiceTChannel();
    WorkflowExecution workflowExecution = new WorkflowExecution();
    String workflowId = args[0];
    workflowExecution.setWorkflowId(workflowId);
    String runId = args[1];
    workflowExecution.setRunId(runId);
    System.out.println(
        WorkflowExecutionUtils.prettyPrintHistory(cadenceService, DOMAIN, workflowExecution, true));
  }
}
