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
package com.uber.cadence.samples.splitmerge;

import com.uber.cadence.WorkflowService;
import com.uber.cadence.client.CadenceClient;
import com.uber.cadence.client.WorkflowExternalResult;
import com.uber.cadence.internal.StartWorkflowOptions;
import com.uber.cadence.samples.common.ConfigHelper;
import com.uber.cadence.WorkflowExecution;

import static com.uber.cadence.samples.splitmerge.SplitMergeWorker.TASK_LIST;

public class SplitMergeStarter {
    private static WorkflowService.Iface swfService;
    private static String domain;

    public static void main(String[] args) throws Exception {

        // Load configuration
        ConfigHelper configHelper = ConfigHelper.createConfig();

        // Create the client for Simple Workflow Service
        swfService = configHelper.createWorkflowClient();
        domain = configHelper.getDomain();

        // Start Workflow execution
        String bucketName = configHelper.getValueFromConfig(SplitMergeConfigKeys.S3_BUCKET_NAME);
        String fileName = configHelper.getValueFromConfig(SplitMergeConfigKeys.S3_INPUT_FILENAME);
        String val = configHelper.getValueFromConfig(SplitMergeConfigKeys.NUMBER_OF_WORKERS);
        int numberOfWorkers = Integer.parseInt(val);

        CadenceClient cadenceClient = CadenceClient.newClient(swfService, domain);
        StartWorkflowOptions startOptions = new StartWorkflowOptions();
        startOptions.setTaskList(TASK_LIST);
        startOptions.setTaskStartToCloseTimeoutSeconds(10);
        startOptions.setExecutionStartToCloseTimeoutSeconds(300);
        AverageCalculatorWorkflow workflow = cadenceClient.newWorkflowStub(AverageCalculatorWorkflow.class, startOptions);
        WorkflowExternalResult<Void> result = CadenceClient.asyncStart(workflow::average, bucketName, fileName, numberOfWorkers);
        WorkflowExecution workflowExecution = result.getExecution();
        System.out.println("Started split-merge workflow with workflowId=\"" + workflowExecution.getWorkflowId()
                + "\" and runId=\"" + workflowExecution.getRunId() + "\"");

        System.exit(0);
    }
}