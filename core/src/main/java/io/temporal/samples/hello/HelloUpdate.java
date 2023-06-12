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

import com.google.common.base.Throwables;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample Temporal workflow that demonstrates how to use workflow update methods to update a
 * workflow execution from external sources. Workflow update is another way to interact with a
 * running workflow along with signals and queries. Workflow update combines aspects of signals and
 * queries. Like signals, workflow update can mutate workflow state. Like queries, workflow update
 * can return a value.
 *
 * <p>Note: Make sure to set {@code frontend.enableUpdateWorkflowExecution=true} in your Temporal
 * config to enabled update.
 */
public class HelloUpdate {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloUpdateTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloUpdateWorkflow";

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
    List<String> getGreetings();

    /*
     * Define the workflow addGreeting update method. This method is executed when the workflow
     * receives an update request.
     */
    @UpdateMethod
    int addGreeting(String name);

    /*
     * Define an optional workflow update validator. The validator must take the same parameters as the update handle.
     * The validator is run before the update handle.
     * If the validator fails by throwing any exception the update request will be rejected and the handle will not run.
     * If the validator passes the update will be considered accepted and the handler will run.
     */
    @UpdateValidatorMethod(updateName = "addGreeting")
    void addGreetingValidator(String name);

    // Define the workflow exit signal method. This method is executed when the workflow receives a
    // signal.
    @SignalMethod
    void exit();
  }

  // Define the workflow implementation which implements the getGreetings workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    // messageQueue holds up to 10 messages (received from updates)
    private final List<String> messageQueue = new ArrayList<>(10);
    private final List<String> receivedMessages = new ArrayList<>(10);
    private boolean exit = false;

    private final HelloActivity.GreetingActivities activities =
        Workflow.newActivityStub(
            HelloActivity.GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public List<String> getGreetings() {

      while (true) {
        // Block current thread until the unblocking condition is evaluated to true
        Workflow.await(() -> !messageQueue.isEmpty() || exit);
        if (messageQueue.isEmpty() && exit) {
          /*
           * no messages in queue and exit signal was sent, return the received messages.
           *
           * Note: A accepted update will not stop workflow completion. If a workflow tries to complete after an update
           * has been sent by a client, but before it has been accepted by the workflow, the workflow will not complete.
           */
          return receivedMessages;
        }
        String message = messageQueue.remove(0);
        receivedMessages.add(message);
      }
    }

    @Override
    public int addGreeting(String name) {
      if (name.isEmpty()) {
        /*
         * Updates can fail by throwing a TemporalFailure. All other exceptions cause the workflow
         * task to fail and potentially retried.
         *
         * Note: A check like this could (and should) belong in the validator, this is just to demonstrate failing an
         * update.
         */
        throw ApplicationFailure.newFailure("Cannot greet someone with an empty name", "Failure");
      }
      // Updates can mutate workflow state like variables or call activities
      messageQueue.add(activities.composeGreeting("Hello", name));
      // Updates can return data back to the client
      return receivedMessages.size() + messageQueue.size();
    }

    @Override
    public void addGreetingValidator(String name) {
      /*
       * Update validators have the same restrictions as Queries. So workflow state cannot be
       * mutated inside a validator.
       */
      if (receivedMessages.size() >= 10) {
        /*
         * Throwing any exception inside an update validator will cause the update to be rejected.
         * Note: rejected update will not appear in the workflow history
         */
        throw new IllegalStateException("Only 10 greetings may be added");
      }
    }

    @Override
    public void exit() {
      exit = true;
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
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new HelloActivity.GreetingActivitiesImpl());

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
    WorkflowClient.start(workflow::getGreetings);

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send an update to it using the workflow stub.
    // This workflow keeps receiving updates until exit is called

    // When the workflow is started the getGreetings will block for the previously defined
    // conditions
    // Send the first workflow update
    workflow.addGreeting("World");

    /*
     * Here we create a new workflow stub using the same workflow id.
     * We do this to demonstrate that to send an update to an already running workflow
     * you only need to know its workflow id.
     */
    GreetingWorkflow workflowById = client.newWorkflowStub(GreetingWorkflow.class, WORKFLOW_ID);

    // Send the second update to our workflow
    workflowById.addGreeting("Universe");

    /*
     * Create an untyped workflow stub to demonstrate sending an update
     * with the untyped stub.
     */
    WorkflowStub greetingStub = client.newUntypedWorkflowStub(WORKFLOW_ID);
    greetingStub.update("addGreeting", int.class, "Temporal");

    try {
      // The update request will fail on a empty name and the exception will be thrown here.
      workflowById.addGreeting("");
      System.exit(-1);
    } catch (WorkflowUpdateException e) {
      Throwable cause = Throwables.getRootCause(e);
      /*
       * Here we should get our originally thrown ApplicationError
       * and the message "Cannot greet someone with an empty name".
       */
      System.out.println("\n Update failed, root cause: " + cause.getMessage());
    }
    // Send our update validators limit of 10 updates
    int sentUpdates = workflowById.addGreeting("Update");
    while (sentUpdates < 10) {
      sentUpdates = workflowById.addGreeting("Again");
    }

    // The update request will be rejected because our validator will fail
    try {
      workflowById.addGreeting("Will be rejected");
      System.exit(-1);
    } catch (WorkflowUpdateException e) {
      Throwable cause = Throwables.getRootCause(e);
      System.out.println("\n Update rejected: " + cause.getMessage());
    }

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
