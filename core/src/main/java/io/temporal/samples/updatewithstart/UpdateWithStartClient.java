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

package io.temporal.samples.updatewithstart;

import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.*;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class UpdateWithStartClient {
  private static final String TASK_QUEUE = "UpdateWithStartTQ";
  private static final String WORKFLOW_ID_PREFIX = "update-with-start-";

  public static void main(String[] args) {
    WorkflowClient client = setupWorkflowClient();
    var opts = buildWorkflowOptions();
    runWorkflowWithUpdateWithStart(client, opts);
    runWorkflowWithUpdateWithStart(client, opts);
  }

  // Set up the WorkflowClient
  public static WorkflowClient setupWorkflowClient() {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    return WorkflowClient.newInstance(service);
  }

  // Run workflow using 'updateWithStart'
  private static void runWorkflowWithUpdateWithStart(
      WorkflowClient client, WorkflowOptions options) {

    var args = new StartWorkflowRequest();

    UpdateWithStartWorkflow workflow =
        client.newWorkflowStub(UpdateWithStartWorkflow.class, options);

    try {
      //      var result =
      //          WorkflowClient.executeUpdateWithStart(
      //              workflow::putApplication,
      //              args,
      //              UpdateOptions.<UpdateWithStartWorkflowState>newBuilder().build(),
      //              new WithStartWorkflowOperation<>(workflow::execute, args));

      var handle =
          WorkflowClient.startUpdateWithStart(
              workflow::putApplication,
              args,
              UpdateOptions.<UpdateWithStartWorkflowState>newBuilder()
                  .setWaitForStage(WorkflowUpdateStage.ACCEPTED)
                  .build(),
              new WithStartWorkflowOperation<>(workflow::execute, args));
      var result = handle.getResult();

      System.out.println(
          "Workflow UwS with value: "
              + result.getArgs().getValue()
              + ", with updates count:"
              + result.getUpdates().size());

    } catch (WorkflowExecutionAlreadyStarted e) {
      System.err.println("WorkflowAlreadyStarted" + e);
    } catch (WorkflowServiceException e) {
      System.err.println("WorkflowServiceException" + e.getCause());
    } catch (Exception e) {
      System.err.println(
          "UpdateWithStart failed: " + e.getMessage() + "/" + e.getClass().getCanonicalName());
    }
  }

  // https://docs.temporal.io/develop/java/message-passing
  // Build WorkflowOptions with task queue and unique ID
  private static WorkflowOptions buildWorkflowOptions() {
    return WorkflowOptions.newBuilder()
        .setTaskQueue(TASK_QUEUE)
        .setWorkflowIdReusePolicy(
            WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
        .setWorkflowIdConflictPolicy(
            WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
        .setWorkflowId(WORKFLOW_ID_PREFIX + System.currentTimeMillis())
        .build();
  }
}
