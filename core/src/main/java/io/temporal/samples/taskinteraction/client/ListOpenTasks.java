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

package io.temporal.samples.taskinteraction.client;

import static io.temporal.samples.taskinteraction.client.StartWorkflow.WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.taskinteraction.Task;
import io.temporal.samples.taskinteraction.TaskClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;

public class ListOpenTasks {

  public static void main(String[] args) throws InterruptedException {

    final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    final WorkflowClient client = WorkflowClient.newInstance(service);

    final TaskClient taskClient = client.newWorkflowStub(TaskClient.class, WORKFLOW_ID);

    List<Task> openTasks = taskClient.getOpenTasks();
    while (openTasks.size() > 0) {

      System.out.println("\nOpen tasks:[" + openTasks.size() + "]");
      openTasks.forEach(
          t -> {
            System.out.println("  " + t);
          });

      Thread.sleep(10000);
      openTasks = taskClient.getOpenTasks();
    }

    System.exit(0);
  }
}
