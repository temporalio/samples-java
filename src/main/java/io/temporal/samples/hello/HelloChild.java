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
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Demonstrates a child workflow. Requires a local instance of the Temporal server to be running.
 */
public class HelloChild {

  static final String TASK_QUEUE = "HelloChild";

  /** The parent workflow interface. */
  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** The child workflow interface. */
  @WorkflowInterface
  public interface GreetingChild {
    @WorkflowMethod
    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#printIt. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    @Override
    public String getGreeting(String name) {
      // Workflows are stateful. So a new stub must be created for each new child.
      GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);

      // This is a non blocking call that returns immediately.
      // Use child.composeGreeting("Hello", name) to call synchronously.
      Promise<String> greeting = Async.function(child::composeGreeting, "Hello", name);
      // Do something else here.
      return greeting.get(); // blocks waiting for the child to complete.
    }
  }

  /**
   * The child workflow implementation. A workflow implementation must always be public for the
   * Temporal library to be able to create instances.
   */
  public static class GreetingChildImpl implements GreetingChild {
    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + "!";
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
    // Start listening to the workflow task queue.
    factory.start();

    // Start a workflow execution. Usually this is done from another program.
    // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    System.out.println(greeting);
    System.exit(0);
  }
}
