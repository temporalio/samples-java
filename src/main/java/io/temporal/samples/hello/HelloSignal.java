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
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample Temporal workflow that demonstrates how to use workflow signal methods to signal from
 * external sources.
 *
 * <p>To execute this example a locally running Temporal service instance is required. You can
 * follow instructions on how to set up your Temporal service here:
 * https://github.com/temporalio/temporal/blob/master/README.md#download-and-start-temporal-server-locally
 */
public class HelloSignal {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloSignalTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloSignalWorkflow";

  /**
   * Define the Workflow Interface. It must contain at least one method annotated
   * with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {
    /**
     * Define the workflow method. This method is executed when the workflow is started. The
     * workflow completes when the workflow method finishes execution.
     */
    @WorkflowMethod
    List<String> getGreetings();

    // Define the workflow waitForName signal method. This method is executed when the workflow
    // receives a signal.
    @SignalMethod
    void waitForName(String name);

    // Define the workflow exit signal method. This method is executed when the workflow receives a
    // signal.
    @SignalMethod
    void exit();
  }

  // Define the workflow implementation. It implements our getGreetings workflow method
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    // messageQueue holds up to 10 messages (received from signals)
    List<String> messageQueue = new ArrayList<>(10);
    boolean exit = false;

    @Override
    public List<String> getGreetings() {
      List<String> receivedMessages = new ArrayList<>(10);

      while (true) {
        // Block current thread until the unblocking condition is evaluated to true
        Workflow.await(() -> !messageQueue.isEmpty() || exit);
        if (messageQueue.isEmpty() && exit) {
          // no messages in queue and exit signal was sent, return our received messages
          return receivedMessages;
        }
        String message = messageQueue.remove(0);
        receivedMessages.add(message);
      }
    }

    @Override
    public void waitForName(String name) {
      messageQueue.add("Hello " + name + "!");
    }

    @Override
    public void exit() {
      exit = true;
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method is our
   * workflow starter.
   */
  public static void main(String[] args) throws Exception {
    /*
     * Define the workflow service. It is a gRPC stubs wrapper which talks to the docker instance of
     * our locally running Temporal service.
     */
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

    /*
     * Define the workflow client. It is a Temporal service client used to start, signal, and query
     * workflows
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
     * Register our workflow implementation with the worker. Since workflows are stateful in nature,
     * we need to register our workflow type.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    // Start all the workers registered for a specific task queue.
    factory.start();

    // Create our workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build();

    // Create our workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously and call its getGreeting workflow method
    WorkflowClient.start(workflow::getGreetings);

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using the workflow stub.
    // This workflow keeps receiving signals until exit is called

    // When our workflow is started the getGreetings will block for the previously defined
    // conditions
    // Send our first signal to our workflow
    workflow.waitForName("World");

    /*
     * Here we create a new workflow stub using the same workflow id.
     * We do this to demonstrate that to send a signal to an already running workflow
     * you only need to know its workflow id.
     */
    GreetingWorkflow workflowById = client.newWorkflowStub(GreetingWorkflow.class, WORKFLOW_ID);

    // Send the second signal to our workflow
    workflowById.waitForName("Universe");

    // Now let's send our exit signal to the workflow
    workflowById.exit();

    /*
     * We now call our getGreetings workflow method synchronously after our workflow has started.
     * This reconnects our workflowById workflow stub to the existing workflow and blocks until
     * a result is available. Note that this behavior assumes that WorkflowOptions are not configured
     * with WorkflowIdReusePolicy.AllowDuplicate. If they were, this call would fail with the
     * WorkflowExecutionAlreadyStartedException exception.
     */
    List<String> greetings = workflowById.getGreetings();

    // Print our two greetings which were sent by signals
    System.out.println(greetings);
    System.exit(0);
  }
}
