/*
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
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;

/**
 * Demonstrates asynchronous signalling of a workflow. Requires a local instance of Cadence server
 * to be running.
 */
@SuppressWarnings("ALL")
public class HelloSignal {

  static final String TASK_LIST = "HelloSignal";

  /** Workflow interface must have a method annotated with @WorkflowMethod. */
  public interface GreetingWorkflow {
    /**
     * @return list of greeting strings that were received through the waitForNameMethod. This
     *     method will block until the number of greetings specified are received.
     */
    @WorkflowMethod
    List<String> getGreetings();

    /** Receives name through an external signal. */
    @SignalMethod
    void waitForName(String name);

    @SignalMethod
    void exit();
  }

  /** GreetingWorkflow implementation that returns a greeting. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    List<String> messageQueue = new ArrayList<>(10);
    boolean exit = false;

    @Override
    public List<String> getGreetings() {
      List<String> receivedMessages = new ArrayList<>(10);

      while (true) {
        Workflow.await(() -> !messageQueue.isEmpty() || exit);
        if (messageQueue.isEmpty() && exit) {
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

  public static void main(String[] args) throws Exception {
    // Start a worker that hosts the workflow implementation.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newInstance(WorkflowServiceStubs.LOCAL_DOCKER_TARGET);
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    factory.start();

    // In a real application use a business ID like customer ID or order ID
    String workflowId = RandomStringUtils.randomAlphabetic(10);

    // Start a workflow execution. Usually this is done from another program.
    // Get a workflow stub using the same task list the worker uses.
    // The newly started workflow is going to have the workflowId generated above.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .setWorkflowId(workflowId)
            .build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    // Start workflow asynchronously to not use another thread to signal.
    WorkflowClient.start(workflow::getGreetings);
    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using the workflow stub.
    // This workflow keeps receiving signals until exit is called
    workflow.waitForName("World"); // sends waitForName signal

    // Create a new stub using the workflowId.
    // This is to demonstrate that to send a signal only the workflowId is required.
    GreetingWorkflow workflowById = client.newWorkflowStub(GreetingWorkflow.class, workflowId);
    workflowById.waitForName("Universe"); // sends waitForName signal
    workflowById.exit(); // sends exit signal
    // Calling synchronous getGreeting after workflow has started reconnects to the existing
    // workflow and blocks until a result is available. Note that this behavior assumes that
    // WorkflowOptions are not configured with WorkflowIdReusePolicy.AllowDuplicate. In that case
    // the call would fail with WorkflowExecutionAlreadyStartedException.
    List<String> greetings = workflowById.getGreetings();
    System.out.println(greetings);
    System.exit(0);
  }
}
