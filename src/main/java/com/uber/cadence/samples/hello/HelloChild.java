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
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Async;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;

/**
 * Demonstrates child workflow.
 * Requires a local instance of Cadence server running.
 */
public class HelloChild {

    private static final String TASK_LIST = "HelloChild";

    /**
     * Workflow interface has to have at least one method annotated with @WorkflowMethod.
     */
    public interface GreetingWorkflow {
        /**
         * @return greeting string
         */
        @WorkflowMethod(executionStartToCloseTimeoutSeconds = 10, taskList = TASK_LIST)
        String getGreeting(String name);
    }

    /**
     * Activity interface is just a POJI
     */
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String greeting, String name);
    }

    /**
     * GreetingWorkflow implementation that calls GreetingsActivities#printIt.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {
            // Workflows are stateful. So new stub must be created for each new child.
            GreetingChild child = Workflow.newWorkflowStub(GreetingChild.class);

            // This is blocking call that returns only after child is completed.
            Promise<String> greeting = Async.function(child::composeGreeting, "Hello", name);
            // Do something else here
            return greeting.get(); // blocks waiting for child to complete
        }
    }

    /**
     * Child workflow implementation.
     * Workflow implementation must always be public for the Cadence to be able to create instances.
     */
    public static class GreetingChildImpl implements GreetingChild {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }

    public static void main(String[] args) {
        // Start a worker that hosts both parent and child workflow implementations.
        Worker worker = new Worker(DOMAIN, TASK_LIST);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
        // Start listening to the workflow task list.
        worker.start();

        // Start a workflow execution. Usually it is done from another program.
        WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
        // Get a workflow stub using the same task list the worker uses.
        GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
        // Execute a workflow waiting for it complete.
        String greeting = workflow.getGreeting("World");
        System.out.println(greeting);
        System.exit(0);
    }
}
