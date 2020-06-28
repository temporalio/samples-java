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

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Demonstrates activities that extend a common interface. The core idea is that an activity
 * interface annotated with {@literal @}{@link ActivityInterface} enumerates all the methods it
 * inherited and declared and generates an activity for each of them. To avoid collisions in
 * activity names (which are by default just method names) the {@link
 * ActivityInterface#namePrefix()} or {@link ActivityMethod#name()} parameters should be used.
 */
public class HelloPolymorphicActivity {

  static final String TASK_QUEUE = "HelloPolymorphicActivity";

  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** Base activity interface. Note that it must not be annotated with @ActivityInterface. */
  public interface GreetingActivity {
    String composeGreeting(String name);
  }

  /**
   * Activity definition interface. Must redefine the name of the composeGreeting activity to avoid
   * collision.
   */
  @ActivityInterface(namePrefix = "Hello_")
  public interface HelloActivity extends GreetingActivity {
    @Override
    String composeGreeting(String name);
  }

  /**
   * Activity definition interface. Must redefine the name of the composeGreeting activity to avoid
   * collision.
   */
  @ActivityInterface(namePrefix = "Bye_")
  public interface ByeActivity extends GreetingActivity {
    @Override
    String composeGreeting(String name);
  }

  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivity[] activities =
        new GreetingActivity[] {
          Workflow.newActivityStub(
              HelloActivity.class,
              ActivityOptions.newBuilder()
                  .setScheduleToCloseTimeout(Duration.ofSeconds(2))
                  .build()),
          Workflow.newActivityStub(
              ByeActivity.class,
              ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(2)).build())
        };

    @Override
    public String getGreeting(String name) {
      StringBuilder result = new StringBuilder();
      for (GreetingActivity activity : activities) {
        result.append(activity.composeGreeting(name));
        result.append('\n');
      }
      return result.toString();
    }
  }

  static class HelloActivityImpl implements HelloActivity {
    @Override
    public String composeGreeting(String name) {
      return "Hello " + name + "!";
    }
  }

  static class ByeActivityImpl implements ByeActivity {
    @Override
    public String composeGreeting(String name) {
      return "Bye " + name + "!";
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
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new HelloActivityImpl(), new ByeActivityImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Start a workflow execution. Usually this is done from another program.
    // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete. See {@link
    // io.temporal.samples.hello.HelloSignal}
    // for an example of starting workflow without waiting synchronously for its result.
    String greeting = workflow.getGreeting("World");
    System.out.println(greeting);
    System.exit(0);
  }
}
