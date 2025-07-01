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

package io.temporal.samples.autoheartbeat;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.CanceledFailure;
import io.temporal.samples.autoheartbeat.activities.AutoActivitiesImpl;
import io.temporal.samples.autoheartbeat.interceptor.AutoHeartbeatWorkerInterceptor;
import io.temporal.samples.autoheartbeat.workflows.AutoWorkflow;
import io.temporal.samples.autoheartbeat.workflows.AutoWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;

public class Starter {
    static final String TASK_QUEUE = "AutoheartbeatTaskQueue";
    static final String WORKFLOW_ID = "AutoHeartbeatWorkflow";

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Configure our auto heartbeat workflow interceptor which will apply
        // AutoHeartbeaterUtil to each activity workflow schedules which has a heartbeat
        // timeout configured
        WorkerFactoryOptions wfo =
                WorkerFactoryOptions.newBuilder()
                        .setWorkerInterceptors(new AutoHeartbeatWorkerInterceptor())
                        .build();

        WorkerFactory factory = WorkerFactory.newInstance(client, wfo);
        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(AutoWorkflowImpl.class);
        worker.registerActivitiesImplementations(new AutoActivitiesImpl());

        factory.start();

        // first run completes execution with autoheartbeat utils
        firstRun(client);
        // second run cancels running (pending) activity via signal (specific scope cancel)
        secondRun(client);
        // third run cancels running execution which cancels activity as well
        thirdRun(client);
        // fourth run turns off autoheartbeat for activities and lets activity time out on heartbeat
        // timeout
        fourthRun(client);

        System.exit(0);
    }

    @SuppressWarnings("unused")
    private static void firstRun(WorkflowClient client) {
        System.out.println("**** First Run: run workflow to completion");
        AutoWorkflow firstRun =
                client.newWorkflowStub(
                        AutoWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        try {
            String firstRunResult = firstRun.exec("Auto heartbeating is cool");
            System.out.println("First run result: " + firstRunResult);
        } catch (Exception e) {
            System.out.println("First run - Workflow exec exception: " + e.getClass().getName());
        }
    }

    @SuppressWarnings("unused")
    private static void secondRun(WorkflowClient client) {
        System.out.println("\n\n**** Second Run: cancel activities via signal");
        AutoWorkflow secondRun =
                client.newWorkflowStub(
                        AutoWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());
        WorkflowClient.start(secondRun::exec, "Auto heartbeating is cool");
        doSleeps(4);
        secondRun.cancelActivity();

        try {
            String secondRunResult = WorkflowStub.fromTyped(secondRun).getResult(String.class);
            System.out.println("Second run result: " + secondRunResult);
        } catch (Exception e) {
            System.out.println("Second run - Workflow exec exception: " + e.getClass().getName());
        }
    }

    @SuppressWarnings("unused")
    private static void thirdRun(WorkflowClient client) {
        System.out.println("\n\n**** Third Run: cancel workflow execution");
        AutoWorkflow thirdRun =
                client.newWorkflowStub(
                        AutoWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());
        WorkflowClient.start(thirdRun::exec, "Auto heartbeating is cool");
        doSleeps(10);
        try {
            WorkflowStub.fromTyped(thirdRun).cancel();
            String thirdRunResult = WorkflowStub.fromTyped(thirdRun).getResult(String.class);
            System.out.println("Third run result: " + thirdRunResult);
        } catch (Exception e) {
            // we are expecting workflow cancelation
            if (e.getCause() instanceof CanceledFailure) {
                System.out.println("Third run - Workflow execution canceled.");
            } else {
                System.out.println("Third run - Workflow exec exception: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unused")
    private static void fourthRun(WorkflowClient client) {
        System.out.println("\n\n**** Fourth Run: cause heartbeat timeout");
        // we disable autoheartbeat via env var
        System.setProperty("sample.disableAutoHeartbeat", "true");
        AutoWorkflow fourth =
                client.newWorkflowStub(
                        AutoWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        try {
            String fourthRunResult = fourth.exec("Auto heartbeating is cool");
            System.out.println("Fourth run result: " + fourthRunResult);
        } catch (Exception e) {
            System.out.println("Fourth run - Workflow exec exception: " + e.getClass().getName());
        }
    }

    private static void doSleeps(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
