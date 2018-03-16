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

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Async;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Demonstrates async invocation of an entire sequence of activities.
 * Requires a local instance of Cadence server running.
 */
public class HelloAsyncLambda {

    private static final String TASK_LIST = "HelloAsyncLambda";

    public interface GreetingWorkflow {
        /**
         * @return greeting string
         */
        @WorkflowMethod
        String getGreeting(String name);
    }

    /**
     * Activity interface is just a POJI
     */
    public interface GreetingActivities {
        String getGreeting();

        String composeGreeting(String greeting, String name);
    }

    /**
     * GreetingWorkflow implementation that calls GreetingsActivities#printIt.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        /**
         * Activity stub implements activity interface and proxies calls to it to Cadence activity invocations.
         * As activities are reentrant only a single stub can be used for multiple activity invocations.
         */
        private final GreetingActivities activities = Workflow.newActivityStub(
                GreetingActivities.class,
                new ActivityOptions.Builder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

        @Override
        public String getGreeting(String name) {
            // Async.invoke accepts not only activity or child workflow method references
            // but lambda functions as well. Behind the scene it allocates a thread
            // to execute it asynchronously.
            Promise<String> result1 = Async.function(() -> {
                String greeting = activities.getGreeting();
                return activities.composeGreeting(greeting, name);
            });
            Promise<String> result2 = Async.function(() -> {
                String greeting = activities.getGreeting();
                return activities.composeGreeting(greeting, name);
            });
            return result1.get() + "\n" + result2.get();
        }
    }

    private static class GreetingActivitiesImpl implements GreetingActivities {

        @Override
        public String getGreeting() {
            return "Hello";
        }

        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }

    public static void main(String[] args) {
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
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions = new WorkflowOptions.Builder()
                .setTaskList(TASK_LIST)
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class,
                workflowOptions);
        // Execute a workflow waiting for it complete.
        String greeting = workflow.getGreeting("World");
        System.out.println(greeting);
        System.exit(0);
    }
}
