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
package com.uber.cadence.samples.hello;

import com.google.common.base.Throwables;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.DuplicateWorkflowException;
import com.uber.cadence.client.UntypedWorkflowStub;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;

import java.time.Duration;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Demonstrates a "cron" workflow that executes activity periodically.
 * Requires a local instance of Cadence server running.
 */
public class HelloPeriodic {

    private static final String TASK_LIST = "HelloPeriodic";

    public interface GreetingWorkflow {
        /**
         * Use single fixed ID to ensure that there is at most one instance running.
         * To run multiple instances  set different ids through
         * WorkflowOptions passed to WorkflowClient.newWorkflowStub call.
         */
        @WorkflowMethod(
                // At most one instance
                workflowId = "HelloPeriodic",
                // To allow starting workflow with the same ID after the previous one has terminated.
                workflowIdReusePolicy = WorkflowIdReusePolicy.AllowDuplicate,
                // Adjust this value to the maximum time workflow is expected to run
                // It usually depends on number of repetitions and interval between them.
                executionStartToCloseTimeoutSeconds = 300,
                taskList = TASK_LIST)
        void greetPeriodically(String name, Duration delay);
    }

    public interface GreetingActivities {
        void greet(String greeting);
    }

    /**
     * GreetingWorkflow implementation that calls {@link #greetPeriodically(String, Duration)} continuously with a
     * specified interval.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        /**
         * This value is so low just to make the example interesting to watch.
         * In real life I would use something like 100 or value that matches a business cycle.
         * For example if it runs once an hour 24 would make sense.
         */
        private final int CONTINUE_AS_NEW_FREQUENCEY = 10;

        private final GreetingActivities activities = Workflow.newActivityStub(
                GreetingActivities.class,
                new ActivityOptions.Builder()
                        .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                        .build());

        /**
         * Stub used to terminate this workflow run and create the next one with the same ID atomically.
         */
        private final GreetingWorkflow continueAsNew = Workflow.newContinueAsNewStub(GreetingWorkflow.class);

        @Override
        public void greetPeriodically(String name, Duration delay) {
            // Loop predefined number of times then continue this workflow as new.
            // It is needed to periodically truncate history size.
            for (int i = 0; i < CONTINUE_AS_NEW_FREQUENCEY; i++) {
                activities.greet("Hello " + name + "!");
                Workflow.sleep(delay);
            }
            // Current workflow run stops executing after this call.
            continueAsNew.greetPeriodically(name, delay);
            // unreachable line
        }
    }

    private static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public void greet(String greeting) {
            System.out.println("From " + Activity.getWorkflowExecution() + ": " + greeting);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Start a worker that hosts both workflow and activity implementation
        Worker worker = new Worker(DOMAIN, TASK_LIST);
        // Workflows are stateful. So need a type to create instances.
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        // Activities are stateless and thread safe. So a shared instance is used.
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        // Start listening to the workflow and activity task lists.
        worker.start();

        // Start a workflow execution. Usually it is done from another program.
        WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
        // To ensure that this daemon type workflow is always running try to start it periodically
        // ignoring duplicated exception.
        // It is only to protect from application level failures.
        // Failures of a workflow worker don't lead to workflow failures.
        WorkflowExecution execution = null;
        while (true) {
            // Print reason of failure of the previous run, before restarting.
            if (execution != null) {
                UntypedWorkflowStub workflow = workflowClient.newUntypedWorkflowStub(execution);
                try {
                    workflow.getResult(Void.class); //
                } catch (WorkflowException e) {
                    System.out.println("Previous instance failed:\n" + Throwables.getStackTraceAsString(e));
                }
            }
            // New stub instance should be created for each new workflow start.
            GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
            try {
                execution = WorkflowClient.asyncStart(workflow::greetPeriodically,
                        "World", Duration.ofSeconds(1));
                System.out.println("Started " + execution);
            } catch (DuplicateWorkflowException e) {
                System.out.println("Still running as " + e.getExecution());
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }
            // This value is so low just for the sample purpose. In production workflow
            // it is usually much higher.
            Thread.sleep(10000);
        }
    }
}
