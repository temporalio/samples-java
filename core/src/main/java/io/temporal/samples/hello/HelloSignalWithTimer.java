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
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.CanceledFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * Sample Temporal workflow that shows receiving signals for a specific time period and then process
 * last one received and continue as new.
 */
public class HelloSignalWithTimer {
  static final String TASK_QUEUE = "HelloSignalWithTimerTaskQueue";
  static final String WORKFLOW_ID = "HelloSignalWithTimerWorkflow";

  @WorkflowInterface
  public interface SignalWithTimerWorkflow {
    @WorkflowMethod
    void execute();

    @SignalMethod
    void newValue(String value);

    @SignalMethod
    void exit();
  }

  @ActivityInterface
  public interface ValueProcessingActivities {
    void processValue(String value);
  }

  public static class SignalWithTimerWorkflowImpl implements SignalWithTimerWorkflow {

    private Logger logger = Workflow.getLogger(SignalWithTimerWorkflowImpl.class);
    private String lastValue = "";
    private CancellationScope timerScope;
    private boolean exit = false;

    private final ValueProcessingActivities activities =
        Workflow.newActivityStub(
            ValueProcessingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public void execute() {
      // Just in case if exit signal is sent as soon as execution is started
      if (exit) {
        return;
      }
      // Start timer in cancellation scope so we can cancel it on exit signal received
      timerScope =
          Workflow.newCancellationScope(
              () -> {
                try {
                  // You can add a signal handler that updates the sleep duration
                  // As it may change via business logic over time
                  // For sample we just hard code it to 5 seconds
                  Workflow.newTimer(Duration.ofSeconds(5)).get();
                } catch (CanceledFailure e) {
                  // Exit signal is received causing cancellation of timer scope and timer
                  // For sample we just log it, you can handle it if needed
                  logger.info("Timer canceled via exit signal");
                }
              });
      timerScope.run();

      // Process last received signal and either exit or ContinueAsNew depending on if we got
      // Exit signal or not
      activities.processValue(lastValue);

      if (exit) {
        return;
      } else {
        SignalWithTimerWorkflow nextRun =
            Workflow.newContinueAsNewStub(SignalWithTimerWorkflow.class);
        nextRun.execute();
      }
    }

    @Override
    public void newValue(String value) {
      // Note that we can receive a signal at the same time workflow is trying to complete or
      // ContinueAsNew. This would cause workflow task failure with UnhandledCommand
      // in order to deliver this signal to our execution.
      // You can choose what to do in this case depending on business logic.
      // For this sample we just ignore it, alternative could be to process it or carry it over
      // to the continued execution if needed.
      lastValue = value;
    }

    @Override
    public void exit() {
      if (timerScope != null) {
        timerScope.cancel("exit received");
      }
      this.exit = true;
    }
  }

  static class ValueProcessingActivitiesImpl implements ValueProcessingActivities {
    @Override
    public void processValue(String value) {
      // Here you would access downstream services to process the value
      // Dummy impl for sample, do nothing
      System.out.println("Processing value: " + value);
    }
  }

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(SignalWithTimerWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ValueProcessingActivitiesImpl());

    factory.start();

    SignalWithTimerWorkflow workflow =
        client.newWorkflowStub(
            SignalWithTimerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());
    // Start execution, this unblocks when its created by service
    WorkflowClient.start(workflow::execute);

    // Send signals 2s apart 12 times (to simulate cancellation on last ContinueAsNew)
    for (int i = 0; i < 12; i++) {
      workflow.newValue("Value " + i);
      sleep(2);
    }
    sleep(1);
    // Send exit signal
    workflow.exit();

    // Wait for execution to complete after receiving exit signal.
    // This should unblock pretty much immediately
    WorkflowStub.fromTyped(workflow).getResult(Void.class);

    System.exit(0);
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000L);
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }
}
