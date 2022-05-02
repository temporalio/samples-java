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

package io.temporal.samples.common;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Optional;

/**
 * Queries a workflow execution using the Temporal query API. Temporal redirects a query to any
 * currently running workflow worker for the workflow type of the requested workflow execution.
 *
 * @author fateev
 */
public class QueryWorkflowExecution {

  public static void main(String[] args) {
    if (args.length < 2 || args.length > 3) {
      System.err.println(
          "Usage: java "
              + QueryWorkflowExecution.class.getName()
              + " <queryType> <workflowId> [<runId>]");
      System.exit(1);
    }
    String queryType = args[0];
    String workflowId = args[1];
    String runId = args.length == 3 ? args[2] : "";

    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkflowExecution workflowExecution =
        WorkflowExecution.newBuilder().setWorkflowId(workflowId).setRunId(runId).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub(workflowExecution, Optional.empty());

    String result = workflow.query(queryType, String.class);

    System.out.println("Query result for " + workflowExecution + ":");
    System.out.println(result);
  }
}
