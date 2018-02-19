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

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.client.CadenceClient;
import com.uber.cadence.client.UntypedWorkflowStub;

/**
 * Simple example utility to query workflow execution using Cadence query API.
 * Cadence redirects query to any currently running workflow worker for the workflow type
 * of the requested workflow execution.
 *
 * @author fateev
 */
public class QueryWorkflowExecution {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: java " + QueryWorkflowExecution.class.getName() +
                    " <queryType> <workflowId> [<runId>]");
            System.exit(1);
        }
        ConfigHelper configHelper = ConfigHelper.createConfig();
        WorkflowService.Iface swfService = configHelper.createWorkflowClient();
        String domain = configHelper.getDomain();

        String queryType = args[0];

        WorkflowExecution workflowExecution = new WorkflowExecution();
        String workflowId = args[1];
        workflowExecution.setWorkflowId(workflowId);
        if (args.length == 3) {
            String runId = args[1];
            workflowExecution.setRunId(runId);
        }
        CadenceClient client = CadenceClient.newInstance(swfService, domain);
        UntypedWorkflowStub workflow = client.newUntypedWorkflowStub(workflowExecution);
        String result = workflow.query(queryType, String.class);

        System.out.println("Query result for " + workflowExecution + ":");
        System.out.println(result);
    }
}
