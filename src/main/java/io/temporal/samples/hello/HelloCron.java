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

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Demonstrates a "cron" workflow that executes activity periodically. Internally each iteration of
 * the workflow creates a new run using "continue as new" feature.
 *
 * <p>Requires a local instance of Temporal server to be running.
 */
public class HelloCron {

  static final String TASK_QUEUE = "HelloCron";
  static final String CRON_WORKFLOW_ID = "HelloCron";

  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    void greet(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {
    void greet(String greeting);
  }

  /**
   * GreetingWorkflow implementation that calls {@link #greet(String)} once. The cron functionality
   * comes from {@link WorkflowOptions.Builder#setCronSchedule(String)} property.
   */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void greet(String name) {
      activities.greet("Hello " + name + "!");
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public void greet(String greeting) {
      System.out.println(
          "From " + Activity.getExecutionContext().getInfo().getWorkflowId() + ": " + greeting);
    }
  }

  public static void main(String[] args) throws InterruptedException {
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
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Sets the cron schedule using the WorkflowOptions.
    // The cron format is parsed by "https://github.com/robfig/cron" library.
    // Besides the standard "* * * * *" format it supports @every and other extensions.
    // Note that unit testing framework doesn't support the extensions.
    // Use single fixed ID to ensure that there is at most one instance running. To run multiple
    // instances set different IDs.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId(CRON_WORKFLOW_ID)
            .setTaskQueue(TASK_QUEUE)
            .setCronSchedule("* * * * *")
            // Execution timeout limits total time. Cron will stop executing after this timeout.
            .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
            // Run timeout limits duration of a single workflow invocation.
            .setWorkflowRunTimeout(Duration.ofMinutes(1))
            .build();
    //        WorkflowOptions.newBuilder().setCronSchedule("@every 2s").build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      WorkflowExecution execution = WorkflowClient.start(workflow::greet, "World");
      System.out.println("Started " + execution);
    } catch (WorkflowExecutionAlreadyStarted e) {
      System.out.println("Already running as " + e.getExecution());
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
