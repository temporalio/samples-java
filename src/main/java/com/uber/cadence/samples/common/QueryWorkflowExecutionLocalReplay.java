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
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.cadence.worker.Worker;

/**
 * Query workflow execution by getting history from Cadence and executing it on a local worker.
 * Use this approach to debug workflow execution in a local environment.
 * 
 * @author fateev
 */
public class QueryWorkflowExecutionLocalReplay {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java " + QueryWorkflowExecutionLocalReplay.class.getName()
                    + "<workflow implementation class> <workflowId> <runId> <queryType>");
            System.exit(1);
        }
        IWorkflowService cadenceService = new WorkflowServiceTChannel();

        WorkflowExecution workflowExecution = new WorkflowExecution();
        String workflowId = args[1];
        workflowExecution.setWorkflowId(workflowId);
        String runId = args[2];
        workflowExecution.setRunId(runId);
        String queryType = args[3];

        String implementationTypeName = args[0];
        @SuppressWarnings("unchecked")
        Class<Object> workflowImplementationType = (Class<Object>) Class.forName(implementationTypeName);
        Worker replayer = new Worker(cadenceService, DOMAIN, null, null);
        replayer.registerWorkflowImplementationTypes(workflowImplementationType);
        System.out.println("Beginning query replay for " + workflowExecution);
        String queryResult = replayer.queryWorkflowExecution(workflowExecution, queryType, String.class);
        System.out.println("Done query replay for " + workflowExecution);
        System.out.println("Query result:\n" + queryResult);
    }
}
