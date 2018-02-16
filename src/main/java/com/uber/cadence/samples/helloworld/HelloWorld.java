/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.uber.cadence.samples.helloworld;

import com.uber.cadence.client.CadenceClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.ActivityOptions;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Hello World Cadence workflow that executes a single activity.
 * Requires local instance of Cadence server running.
 */
public class HelloWorld {

    private static final String TASK_LIST = "HelloWorld";

    /**
     * Workflow interface has to have at least one method annotated with @WorkflowMethod.
     */
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
        String composeGreeting(String greeting, String name);
    }

    /**
     * GreetingWorkflow implementation that calls GreetingsActivities#printIt.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        private final HelloWorld.GreetingActivities activities = Workflow.newActivityStub(
                GreetingActivities.class,
                new ActivityOptions.Builder().setScheduleToCloseTimeoutSeconds(10).build());

        @Override
        public String getGreeting(String name) {
            // This is blocking call that returns only after
            return activities.composeGreeting("Hello", name );
        }
    }

    public static void main(String[] args) {
        // Start a worker that hosts both workflow and activity implementation
        Worker worker = new Worker(DOMAIN, TASK_LIST);
        // Workflows are stateful. So need a type to create instances.
        worker.addWorkflowImplementationType(HelloWorld.GreetingWorkflowImpl.class);
        // Activities are stateless and thread safe. So a shared instance is used.
        worker.addActivitiesImplementation((GreetingActivities) (greeting, name) -> greeting + " " + name + "!");
        // Start listening to the workflow and activity task lists.
        worker.start();

        // Start a workflow execution. Usually it is done from another program.
        CadenceClient cadenceClient = CadenceClient.newClient(DOMAIN);
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions = new WorkflowOptions.Builder()
                .setTaskList(TASK_LIST)
                .setExecutionStartToCloseTimeoutSeconds(30)
                .build();
        GreetingWorkflow workflow = cadenceClient.newWorkflowStub(GreetingWorkflow.class,
                workflowOptions);
        // Execute a workflow waiting for it complete.
        String greeting = workflow.getGreeting("World");
        System.out.println(greeting);
        System.exit(0);
    }
}
