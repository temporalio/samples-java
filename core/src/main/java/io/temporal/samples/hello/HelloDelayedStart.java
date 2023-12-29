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

package io.temporal.samples.hello;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/** Sample Temporal Workflow Definition that shows how to use delayed start. */
public class HelloDelayedStart {
  // Define the task queue name
  static final String TASK_QUEUE = "HelloDelayedStartTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloDelayedStartWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface DelayedStartWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    void start();
  }

  // Define the workflow implementation which implements our start workflow method.
  public static class DelayedStartWorkflowImpl implements DelayedStartWorkflow {
    @Override
    public void start() {
      // this workflow just sleeps for a second
      Workflow.sleep(Duration.ofSeconds(1));
    }
  }

  public static void main(String[] args) {
    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(DelayedStartWorkflowImpl.class);

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    DelayedStartWorkflow workflow =
        client.newWorkflowStub(
            DelayedStartWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                // set delayed start in 2 seconds
                .setStartDelay(Duration.ofSeconds(2))
                .build());

    workflow.start();

    // Delayed executions are still created right away by the service but
    // they have a firstWorkflowTaskBackoff set to the delay duration
    // meaning the first workflow task is dispatched by service
    // 2 second after exec is started in our sample
    WorkflowExecutionHistory history = client.fetchHistory(WORKFLOW_ID);
    com.google.protobuf.Duration backoff =
        history
            .getHistory()
            .getEvents(0)
            .getWorkflowExecutionStartedEventAttributes()
            .getFirstWorkflowTaskBackoff();
    System.out.println("First workflow task backoff: " + backoff.getSeconds());

    System.exit(0);
  }
}
