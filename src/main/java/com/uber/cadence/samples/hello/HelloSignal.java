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

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.CompletablePromise;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Hello World Cadence workflow that blocks until a signal is received.
 * Requires a local instance of Cadence server running.
 */
@SuppressWarnings("ALL")
public class HelloSignal {

    private static final String TASK_LIST = "HelloSignal";

    /**
     * Workflow interface has to have at least one method annotated with @WorkflowMethod.
     */
    public interface GreetingWorkflow {
        /**
         * @return greeting string
         */
        @WorkflowMethod
        String getGreeting();

        /**
         * Receives name through an external signal.
         */
        @SignalMethod
        void waitForName(String name);
    }

    /**
     * GreetingWorkflow implementation that returns a greeting.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        private final CompletablePromise<String> name = Workflow.newPromise();

        @Override
        public String getGreeting() {
            return "Hello " + name.get() + "!";
        }

        @Override
        public void waitForName(String name) {
            this.name.complete(name);
        }
    }

    public static void main(String[] args) {
        // Start a worker that hosts the workflow implementation
        Worker worker = new Worker(DOMAIN, TASK_LIST);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.start();

        // Start a workflow execution. Usually it is done from another program.
        WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions = new WorkflowOptions.Builder()
                .setTaskList(TASK_LIST)
                .setExecutionStartToCloseTimeoutSeconds(30)
                .build();
        GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class,
                workflowOptions);
        // Start workflow asynchronously to not use another thread to signal.
        WorkflowClient.asyncStart(workflow::getGreeting);
        // After asyncStart for getGreeting returns the workflow is guaranteed to be started.
        // So we can send signal to it using workflow stub.
        workflow.waitForName("World");
        // Calling synchronous getGreeting after workflow has started reconnects to the existing workflow and
        // blocks until result is available. Note that this behavior assumes that WorkflowOptions are not configured
        // with WorkflowIdReusePolicy.AllowDuplicate. In that case the call would fail with WorkflowExecutionAlreadyStartedException.
        String greeting = workflow.getGreeting();
        System.out.println(greeting);
        System.exit(0);
    }
}
