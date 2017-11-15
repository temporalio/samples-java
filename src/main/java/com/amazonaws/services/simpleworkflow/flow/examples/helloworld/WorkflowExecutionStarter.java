/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.simpleworkflow.flow.examples.helloworld;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.uber.cadence.WorkflowService;
import com.amazonaws.services.simpleworkflow.flow.examples.common.ConfigHelper;
import com.uber.cadence.WorkflowExecution;

import java.util.UUID;

public class WorkflowExecutionStarter {

    public static void main(String[] args) throws Exception {
        ConfigHelper configHelper = ConfigHelper.createConfig();
        WorkflowService.Iface swfService = configHelper.createWorkflowClient();
        String domain = configHelper.getDomain();

        HelloWorldWorkflowClientExternalFactory clientFactory = new HelloWorldWorkflowClientExternalFactoryImpl(swfService,
                domain);
        HelloWorldWorkflowClientExternal workflow = clientFactory.getClient(UUID.randomUUID().toString());
        
        // Start Wrokflow Execution
        StartWorkflowOptions options = new StartWorkflowOptions();
        options.setTaskList(WorkflowHost.DECISION_TASK_LIST);
        options.setExecutionStartToCloseTimeoutSeconds(20);
        options.setTaskStartToCloseTimeoutSeconds(3);
        workflow.helloWorld("User", options);
        
        // WorkflowExecution is available after workflow creation 
        WorkflowExecution workflowExecution = workflow.getWorkflowExecution();
        System.out.println("Started helloWorld workflow with workflowId=\"" + workflowExecution.getWorkflowId()
                + "\" and runId=\"" + workflowExecution.getRunId() + "\"");
    }

}
