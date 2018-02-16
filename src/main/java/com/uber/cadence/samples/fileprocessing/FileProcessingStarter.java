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
package com.uber.cadence.samples.fileprocessing;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.client.CadenceClient;
import com.uber.cadence.client.WorkflowExternalResult;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.internal.WorkflowOptions;
import com.uber.cadence.samples.common.ConfigHelper;

/**
 * This is used for launching a Workflow instance of FileProcessingWorkflowExample
 */
public class FileProcessingStarter {

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

        FileProcessingWorkflow.Arguments workflowArgs = new FileProcessingWorkflow.Arguments();
        workflowArgs.setSourceBucketName(sourceBucketName);
        workflowArgs.setSourceFilename(sourceFilename);
        workflowArgs.setTargetBucketName(targetBucketName);
        workflowArgs.setTargetFilename(targetFilename);

        CadenceClient cadenceClient = CadenceClient.newClient(cadenceServiceHost, cadenceServicePort, domain);
        WorkflowOptions options = new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeoutSeconds(300)
                .setTaskList(WORKFLOW_TASK_LIST)
                .build();
        FileProcessingWorkflow workflow = cadenceClient.newWorkflowStub(FileProcessingWorkflow.class, options);
        String result = workflow.processFile(workflowArgs);

        WorkflowExternalResult<Void> result = CadenceClient.asyncStart(workflow::processFile, workflowArgs);

        // WorkflowExecution is available after workflow creation 
        WorkflowExecution workflowExecution = result.getExecution();
        System.out.println("Started periodic workflow with workflowId=\"" + workflowExecution.getWorkflowId()
                + "\" and runId=\"" + workflowExecution.getRunId() + "\"");

        System.exit(0);
    }
}
