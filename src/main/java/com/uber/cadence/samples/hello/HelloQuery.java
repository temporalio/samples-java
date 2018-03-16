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

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Demonstrates query capability.
 * Requires a local instance of Cadence server running.
 */
public class HelloQuery {

    private static final String TASK_LIST = "HelloQuery";

    public interface GreetingWorkflow {

        @WorkflowMethod
        void createGreeting(String name);

        /**
         * Returns greeting as a query value.
         */
        @QueryMethod
        String queryGreeting();
    }

    /**
     * GreetingWorkflow implementation that updates greeting after sleeping for 5 seconds.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        private String greeting;

        @Override
        public void createGreeting(String name) {
            greeting = "Hello " + name + "!";
            // Inside workflow code always use WorkflowThread.sleep
            // and Workflow.currentTimeMillis instead of standard Java ones.
            Workflow.sleep(Duration.ofSeconds(2));
            greeting = "Bye " + name + "!";
        }

        @Override
        public String queryGreeting() {
            return greeting;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Start a worker that hosts the workflow implementation
        Worker worker = new Worker(DOMAIN, TASK_LIST);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
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
        // Start workflow asynchronously to not use another thread to query.
        WorkflowClient.start(workflow::createGreeting, "World");
        // After start for getGreeting returns the workflow is guaranteed to be started.
        // So we can send signal to it using workflow stub.

        System.out.println(workflow.queryGreeting()); // Should print Hello...
        // Note that inside a workflow only WorkflowThread.sleep is allowed. Outside WorkflowThread.sleep is not allowed.
        Thread.sleep(2500);
        System.out.println(workflow.queryGreeting()); // Should print Bye ...
        System.exit(0);
    }
}
