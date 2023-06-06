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

package io.temporal.samples.getresultsasync;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.util.concurrent.TimeUnit;

public class Starter {

  /**
   * Show the use and difference between getResult and getResultAsync for waiting on workflow
   * results.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static void main(String[] args) {
    MyWorkflow workflowStub1 =
        Worker.client.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(Worker.TASK_QUEUE_NAME).build());

    MyWorkflow workflowStub2 =
        Worker.client.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(Worker.TASK_QUEUE_NAME).build());

    // Start workflow async (not blocking thread)
    WorkflowClient.start(workflowStub1::justSleep, 3);
    WorkflowStub untypedStub1 = WorkflowStub.fromTyped(workflowStub1);

    // Get the results, waiting for workflow to complete
    String result1 = untypedStub1.getResult(String.class); // blocking call, waiting to complete
    System.out.println("Result1: " + result1);

    // Start the workflow again (async)
    WorkflowClient.start(workflowStub2::justSleep, 5);
    WorkflowStub untypedStub2 = WorkflowStub.fromTyped(workflowStub2);

    // getResultAsync returns a CompletableFuture
    // It is not a blocking call like getResult(...)
    untypedStub2
        .getResultAsync(String.class)
        .thenApply(
            result2 -> {
              System.out.println("Result2: " + result2);
              return result2;
            });

    System.out.println("Waiting on result2...");
    // Our workflow sleeps for 5 seconds (async)
    // Here we block the thread (Thread.sleep) for 7 (2 more than the workflow exec time)
    // To show that getResultsAsync completion happens during this time (async)
    sleep(7);
    System.out.println("Done waiting on result2...");
    System.exit(0);
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      System.out.println("Exception: " + e.getMessage());
      System.exit(0);
    }
  }
}
