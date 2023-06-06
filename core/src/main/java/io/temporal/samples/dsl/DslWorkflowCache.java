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

package io.temporal.samples.dsl;

import static io.temporal.samples.dsl.utils.DslWorkflowUtils.getFileAsString;

import io.serverlessworkflow.api.Workflow;
import java.util.HashMap;
import java.util.Map;

/** Class that loads up all the DSL workflows and allows access via id-version */
public class DslWorkflowCache {

  private static class WorkflowHolder {
    static final Map<String, Workflow> dslWorkflowMap = new HashMap<>();

    static {
      try {
        Workflow customerApplicationWorkflow =
            Workflow.fromSource(getFileAsString("dsl/customerapplication/workflow.yml"));
        Workflow bankingTransactionsWorkflow =
            Workflow.fromSource(getFileAsString("dsl/bankingtransactions/workflow.yml"));
        Workflow applicantWorkflow =
            Workflow.fromSource(getFileAsString("dsl/customerapproval/applicantworkflow.json"));
        Workflow approvalWorkflow =
            Workflow.fromSource(getFileAsString("dsl/customerapproval/approvalworkflow.json"));
        Workflow bankingParentWorkflow =
            Workflow.fromSource(
                getFileAsString("dsl/bankingtransactionssubflow/parentworkflow.json"));
        Workflow bankingChildWorkflow =
            Workflow.fromSource(
                getFileAsString("dsl/bankingtransactionssubflow/childworkflow.json"));

        dslWorkflowMap.put(
            customerApplicationWorkflow.getId() + "-" + customerApplicationWorkflow.getVersion(),
            customerApplicationWorkflow);
        dslWorkflowMap.put(
            bankingTransactionsWorkflow.getId() + "-" + bankingTransactionsWorkflow.getVersion(),
            bankingTransactionsWorkflow);
        dslWorkflowMap.put(
            applicantWorkflow.getId() + "-" + applicantWorkflow.getVersion(), applicantWorkflow);
        dslWorkflowMap.put(
            approvalWorkflow.getId() + "-" + approvalWorkflow.getVersion(), approvalWorkflow);
        dslWorkflowMap.put(
            bankingParentWorkflow.getId() + "-" + bankingParentWorkflow.getVersion(),
            bankingParentWorkflow);
        dslWorkflowMap.put(
            bankingChildWorkflow.getId() + "-" + bankingChildWorkflow.getVersion(),
            bankingChildWorkflow);
      } catch (Exception e) {
        System.out.println("Exception: " + e.getMessage());
      }
    }
  }

  public static Workflow getWorkflow(String workflowId, String workflowVersion) {
    return WorkflowHolder.dslWorkflowMap.get(workflowId + "-" + workflowVersion);
  }
}
