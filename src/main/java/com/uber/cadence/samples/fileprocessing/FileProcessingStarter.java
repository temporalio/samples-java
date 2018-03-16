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
package com.uber.cadence.samples.fileprocessing;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

import com.uber.cadence.client.WorkflowClient;
import java.net.URL;

/**
 * This is used for launching a Workflow instance of file processing sample.
 */
public class FileProcessingStarter {

    public static void main(String[] args) throws Exception {
        // Start Workflow instance
        WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
        FileProcessingWorkflow workflow = workflowClient.newWorkflowStub(FileProcessingWorkflow.class);

        System.out.println("Executing FileProcessingWorkflow");

        URL source = new URL("http://www.google.com/");
        URL destination = new URL("http://dummy");

        // This is going to block until the workflow completion.
        // This is rarely used in production. Use the commented code below for async start version.
        workflow.processFile(source, destination);
        System.out.println("FileProcessingWorkflow completed");

        // Use this code instead of the above blocking call to start workflow asynchronously.
//        WorkflowExecution workflowExecution = WorkflowClient.start(workflow::processFile, source, destination);
//        System.out.println("Started periodic workflow with workflowId=\"" + workflowExecution.getWorkflowId()
//                + "\" and runId=\"" + workflowExecution.getRunId() + "\"");
//
        System.exit(0);
    }
}
