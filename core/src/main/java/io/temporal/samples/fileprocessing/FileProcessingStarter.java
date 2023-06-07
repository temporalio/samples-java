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

package io.temporal.samples.fileprocessing;

import static io.temporal.samples.fileprocessing.FileProcessingWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.net.URL;

/** Starts a file processing sample workflow. */
public class FileProcessingStarter {

  public static void main(String[] args) throws Exception {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);
    FileProcessingWorkflow workflow =
        client.newWorkflowStub(
            FileProcessingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    System.out.println("Executing FileProcessingWorkflow");

    URL source = new URL("http://www.google.com/");
    URL destination = new URL("http://dummy");

    // This is going to block until the workflow completes.
    // This is rarely used in production. Use the commented code below for async start version.
    workflow.processFile(source, destination);
    System.out.println("FileProcessingWorkflow completed");

    // Use this code instead of the above blocking call to start workflow asynchronously.
    //    WorkflowExecution workflowExecution =
    //        WorkflowClient.start(workflow::processFile, source, destination);
    //    System.out.println(
    //        "Started periodic workflow with workflowId=\""
    //            + workflowExecution.getWorkflowId()
    //            + "\" and runId=\""
    //            + workflowExecution.getRunId()
    //            + "\"");
    //
    System.exit(0);
  }
}
