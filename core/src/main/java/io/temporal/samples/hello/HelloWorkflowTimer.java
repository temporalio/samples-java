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

import io.temporal.activity.*;
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;

/** Sample shows how to use workflow timer instead of WorkflowOptions->Run/ExecutionTimeout */
public class HelloWorkflowTimer {
  private static String WORKFLOW_ID = "HelloWorkflowWithTimer";
  private static String TASK_QUEUE = "HelloWorkflowWithTimerTaskQueue";
  // Change time to 12 to 20 seconds to handle cancellation while child workflow is running
  private static int TIME_SECS = 8;

  // Workflow
  @WorkflowInterface
  public interface WorkflowWithTimer {
    @WorkflowMethod
    String execute(String input);
  }

  public static class WorkflowWithTimerImpl implements WorkflowWithTimer {
    // Our timer cancellation scope
    private CancellationScope timerCancellationScope;
    // Our workflow cancellation scope
    private CancellationScope workflowCancellationScope;
    // Workflow result
    private String workflowResult = "";
    private Promise<Void> workflowTimerPromise;

    @Override
    public String execute(String input) {
      // Create workflow timer (within timer cancel;ation scope so it can be canceled)
      // which denotes the max amount of time we allow this execution to run
      // Using workflow timer instead of workflow run/execution timeouts allow us to react to this
      // timer
      // fires, be able to chose if we want to fail or complete execution, and do some "cleanup"
      // tasks if
      // necessary before we do so. If we used workflow run/execution timeouts insted we would not
      // be able
      // to react to this timer firing (its server timer only)
      timerCancellationScope =
          Workflow.newCancellationScope(
              () -> {
                workflowTimerPromise =
                    Workflow.newTimer(
                            Duration.ofSeconds(TIME_SECS),
                            TimerOptions.newBuilder().setSummary("Workflow Timer").build())
                        // We can use thenApply here to cancel our cancelation scope when this timer
                        // fires. Note we cannot complete the execution from here, see
                        // https://github.com/temporalio/sdk-java/issues/87
                        .thenApply(
                            ignore -> {
                              // Cancel the workflow cancellation scope allowing us to react to this
                              // timer firing
                              if (workflowCancellationScope != null) {
                                workflowCancellationScope.cancel("Workflow timer fired");
                              }
                              return null;
                            });
              });
      timerCancellationScope.run();

      // Create workflow cancellation scope in which we put our core business logic
      workflowCancellationScope =
          Workflow.newCancellationScope(
              () -> {
                WorkflowWithTimerActivities activities =
                    Workflow.newActivityStub(
                        WorkflowWithTimerActivities.class,
                        ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(12))
                            // Set heartbeat timeout to 1s
                            .setHeartbeatTimeout(Duration.ofSeconds(2))
                            // We want to wait for activity to complete cancellation
                            .setCancellationType(
                                ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                            .build());

                WorkflowWithTimerChildWorkflow childWorkflow =
                    Workflow.newChildWorkflowStub(
                        WorkflowWithTimerChildWorkflow.class,
                        ChildWorkflowOptions.newBuilder()
                            .setWorkflowId(WORKFLOW_ID + "-Child")
                            // We want to wait for child workflow cancellation completion
                            .setCancellationType(
                                ChildWorkflowCancellationType.WAIT_CANCELLATION_COMPLETED)
                            .build());

                try {
                  // Run our activities
                  workflowResult = activities.sayHello(input);
                  // Then our child workflow
                  childWorkflow.executeChild(input);
                } catch (ActivityFailure af) {
                  // Handle cancellation of scope while activities are pending (running)
                  if (af.getCause() instanceof CanceledFailure) {
                    workflowResult = "Workflow timer fired while activities were executing.";
                    // Here we can do more work if needed
                  }
                } catch (ChildWorkflowFailure cwf) {
                  // Handle cancellation of scope while child workflow is pending (running)
                  if (cwf.getCause() instanceof CanceledFailure) {
                    workflowResult = "Workflow timer fired while child workflow was executing.";
                    // Here we can do more work if needed
                  }
                }
              });
      // Run the workflow cancellation scope
      // We need to handle CanceledFailure here in case we cancel the scope
      // right before activity/child workflows are scheduled
      try {
        workflowCancellationScope.run();
      } catch (CanceledFailure e) {
        workflowResult = "Workflow cancelled.";
      }

      // Cancel our workflow timer if it didnt fire
      if (!workflowTimerPromise.isCompleted()) {
        timerCancellationScope.cancel("Workflow completed before workflow timer.");
      }

      return workflowResult;
    }
  }

  // Activities
  @ActivityInterface
  public interface WorkflowWithTimerActivities {
    String sayHello(String input);
  }

  public static class WorkflowWithTimerActivitiesImpl implements WorkflowWithTimerActivities {
    @Override
    public String sayHello(String input) {
      // here we just heartbeat then sleep for 1s
      for (int i = 0; i < 10; i++) {
        try {
          Activity.getExecutionContext().heartbeat("heartbeating: " + i);
        } catch (ActivityCompletionException e) {
          // Do some cleanup if needed, then re-throw
          throw e;
        }
        sleep(1);
      }
      return "Hello " + input;
    }

    // Just sample sleep method
    private void sleep(int seconds) {
      try {
        Thread.sleep(seconds * 1000L);
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }
  }

  // Child Workflows
  @WorkflowInterface
  public interface WorkflowWithTimerChildWorkflow {
    @WorkflowMethod
    String executeChild(String input);
  }

  public static class WorkflowWithTimerChildWorkflowImpl implements WorkflowWithTimerChildWorkflow {
    @Override
    public String executeChild(String input) {
      // For sample we just sleep for 5 seconds and return some result
      try {
        Workflow.sleep(Duration.ofSeconds(5));
        return "From executeChild - " + input;
        // Note that similarly to parent workflow if child is running activities/child workflows
        // we need to handle this in same way as parent does
        // Fpr sample we can just handle CanceledFailure and rethrow
      } catch (CanceledFailure e) {
        // Can do cleanup if needed
        throw e;
      }
    }
  }

  public static void main(String[] args) {
    // Create service stubs
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // Crete workflow client
    WorkflowClient client = WorkflowClient.newInstance(service);
    // Create worker factory
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Create worker
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Register workflow and child workflow
    worker.registerWorkflowImplementationTypes(
        WorkflowWithTimerImpl.class, WorkflowWithTimerChildWorkflowImpl.class);
    // Register activities
    worker.registerActivitiesImplementations(new WorkflowWithTimerActivitiesImpl());

    // Start factory (and worker)
    factory.start();

    // Create workflow stub
    WorkflowWithTimer workflow =
        client.newWorkflowStub(
            WorkflowWithTimer.class,
            WorkflowOptions.newBuilder()
                // Note we do not set workflow run/execution timeouts
                // As its not recommended in most cases
                // In same we show how we can implement this with workflow timer instead
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    // Start workflow execution async
    WorkflowClient.start(workflow::execute, "Some Name Here");

    // Wait for execution to complete (sync)
    WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
    String result = workflowStub.getResult(String.class);
    System.out.println("Workflow result: " + result);

    // Stop main method
    System.exit(0);
  }
}
