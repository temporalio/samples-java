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
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Sample Temporal workflow that demonstrates how to use workflow await methods to wait up to a
 * specified timeout for a condition updated from a signal handler.
 */
public class HelloAwait {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloAwaitTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloAwaitWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see WorkflowInterface
   * @see WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {
    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting();

    // Define the workflow waitForName signal method. This method is executed when the workflow
    // receives a "WaitForName" signal.
    @SignalMethod
    void waitForName(String name);
  }

  // Define the workflow implementation which implements the getGreetings workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private String name;

    @Override
    public String getGreeting() {
      boolean ok = Workflow.await(Duration.ofSeconds(10), () -> name != null);
      if (ok) {
        return "Hello " + name + "!";
      } else {
        // To fail workflow use ApplicationFailure. Any other exception would cause workflow to
        // stall, not to fail.
        throw ApplicationFailure.newFailure(
            "WaitForName signal is not received within 10 seconds.", "signal-timeout");
      }
    }

    @Override
    public void waitForName(String name) {
      this.name = name;
    }
  }

  /**
   * With the Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws Exception {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Await, and Query Workflow Executions.
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
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create the workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build();

    // Create the workflow client stub. It is used to start the workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously and call its getGreeting workflow method
    WorkflowClient.start(workflow::getGreeting);

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // Send WaitForName signal.
    workflow.waitForName("World");

    /*
     * Here we create a new untyped workflow stub using the same workflow id.
     * The untyped stub is a convenient way to wait for a workflow result.
     */
    WorkflowStub workflowById = client.newUntypedWorkflowStub(WORKFLOW_ID);

    String greeting = workflowById.getResult(String.class);

    System.out.println(greeting);
    System.exit(0);
  }
}
