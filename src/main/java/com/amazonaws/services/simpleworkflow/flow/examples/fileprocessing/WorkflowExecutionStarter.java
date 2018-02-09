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
package com.amazonaws.services.simpleworkflow.flow.examples.fileprocessing;

import com.amazonaws.services.simpleworkflow.flow.examples.common.ConfigHelper;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.client.CadenceClient;
import com.uber.cadence.client.WorkflowExternalResult;
import com.uber.cadence.internal.StartWorkflowOptions;

/**
 * This is used for launching a Workflow instance of FileProcessingWorkflowExample
 */
public class WorkflowExecutionStarter {

    static final String WORKFLOW_TASK_LIST = "FileProcessing";

    private static WorkflowService.Iface swfService;
    private static String domain;
    
    public static void main(String[] args) throws Exception {

    	// Load configuration
    	ConfigHelper configHelper = ConfigHelper.createConfig();
        
        // Create the client for Simple Workflow Service
        swfService = configHelper.createWorkflowClient();
        domain = configHelper.getDomain();
        
        // Start Workflow instance
        String sourceBucketName = configHelper.getValueFromConfig(FileProcessingConfigKeys.WORKFLOW_INPUT_SOURCEBUCKETNAME_KEY);
        String sourceFilename = configHelper.getValueFromConfig(FileProcessingConfigKeys.WORKFLOW_INPUT_SOURCEFILENAME_KEY);
        String targetBucketName = configHelper.getValueFromConfig(FileProcessingConfigKeys.WORKFLOW_INPUT_TARGETBUCKETNAME_KEY);
        String targetFilename = configHelper.getValueFromConfig(FileProcessingConfigKeys.WORKFLOW_INPUT_TARGETFILENAME_KEY);
        
        CadenceClient clientFactory = CadenceClient.newClient(swfService, domain);
        StartWorkflowOptions startOptions = new StartWorkflowOptions();
        startOptions.setExecutionStartToCloseTimeoutSeconds(300);
        startOptions.setTaskStartToCloseTimeoutSeconds(10);
        startOptions.setTaskList(WORKFLOW_TASK_LIST);
        FileProcessingWorkflow workflow = clientFactory.newWorkflowStub(FileProcessingWorkflow.class, startOptions);
        FileProcessingWorkflow.Arguments args = new FileProcessingWorkflow.Arguments();
        args.setSourceBucketName(sourceBucketName);
        args.setSourceFilename(sourceFilename);
        args.setTargetBucketName(targetBucketName);
        args.setTargetFilename(targetFilename);

        WorkflowExternalResult<Void> result = CadenceClient.asyncStart(workflow::processFile, args);

        // WorkflowExecution is available after workflow creation 
        WorkflowExecution workflowExecution = result.getExecution();
        System.out.println("Started periodic workflow with workflowId=\"" + workflowExecution.getWorkflowId()
                + "\" and runId=\"" + workflowExecution.getRunId() + "\"");

        System.exit(0);
    }    
}
