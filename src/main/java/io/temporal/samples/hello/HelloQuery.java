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
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/** Sample Temporal Workflow Definition that demonstrates how to Query a Workflow. */
public class HelloQuery {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloQueryTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloQueryWorkflow";

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
  public interface GreetingWorkflow {

    @WorkflowMethod
    void createGreeting(String name);

    // Workflow query method. Used to return our greeting as a query value
    @QueryMethod
    String queryGreeting();
  }

  // Define the workflow implementation which implements our createGreeting and
  // queryGreeting workflow methods.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private String greeting;

    @Override
    public void createGreeting(String name) {
      // We set the value of greeting to "Hello" first.
      greeting = "Hello " + name + "!";
      /*
       * Note that our createGreeting workflow method has return type of void.
       * It only sets the greeting and does not return it.
       *
       * Also note that inside a workflow method you should always
       * use Workflow.sleep or Workflow.currentTimeMillis rather than the
       * equivalent standard Java ones.
       */
      Workflow.sleep(Duration.ofSeconds(2));

      // after two seconds we change the value of our greeting to "Bye"
      greeting = "Bye " + name + "!";
    }

    // our workflow query method returns the greeting
    @Override
    public String queryGreeting() {
      return greeting;
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws InterruptedException {

    // Define the workflow service.
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
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create our workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TASK_QUEUE).build();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start our workflow asynchronously to not use another thread to query.
    WorkflowClient.start(workflow::createGreeting, "World");

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub.

    /*
     * Query our workflow to get the current value of greeting.
     * Remember that original the workflow methods sets this value to "Hello"
     * So here we should get "Hello World".
     */
    System.out.println(workflow.queryGreeting());

    /*
     * Sleep for 2.5 seconds. This value is set because remember in our
     * workflow method the value of the greeting is updated after 2 seconds.
     *
     * Also note since here we are not inside a Workflow method, we can use the
     * standard Java Thread.sleep.
     */
    Thread.sleep(2500);

    /*
     * Query our workflow to get the current value of greeting.
     *
     * Now we should get "Bye World".
     */
    System.out.println(workflow.queryGreeting());
    System.exit(0);
  }
}
