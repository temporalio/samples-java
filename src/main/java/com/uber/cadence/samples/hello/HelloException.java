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
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.ActivityOptions;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Demonstrates exception propagation across activity, child workflow and workflow client boundaries.
 * Shows how to deal with checked exceptions.
 * <li>
 * <ul>
 * Exceptions thrown by an activity are received by the workflow wrapped into an {@link com.uber.cadence.workflow.ActivityFailureException}.
 * </ul>
 * <ul>
 * Exceptions thrown by a child workflow are received by a parent workflow wrapped into a {@link com.uber.cadence.workflow.ChildWorkflowFailureException}.
 * </ul>
 * <ul>
 * Exceptions thrown by a workflow are received  by a workflow client wrapped into {@link com.uber.cadence.client.WorkflowFailureException}.
 * </ul>
 * </li>
 * <p>
 * In this example a Workflow Client executes a workflow which executes a child workflow
 * which executes activity which throws an IOException. The resulting exception stack trace is:
 * <pre>
 * com.uber.cadence.client.WorkflowFailureException: WorkflowType="GreetingWorkflow::getGreeting", WorkflowID="38b9ce7a-e370-4cd8-a9f3-35e7295f7b3d", RunID="37ceb58c-9271-4fca-b5aa-ba06c5495214
 *     at com.uber.cadence.internal.dispatcher.UntypedWorkflowStubImpl.getResult(UntypedWorkflowStubImpl.java:139)
 *     at com.uber.cadence.internal.dispatcher.UntypedWorkflowStubImpl.getResult(UntypedWorkflowStubImpl.java:111)
 *     at com.uber.cadence.internal.dispatcher.WorkflowExternalInvocationHandler.startWorkflow(WorkflowExternalInvocationHandler.java:187)
 *     at com.uber.cadence.internal.dispatcher.WorkflowExternalInvocationHandler.invoke(WorkflowExternalInvocationHandler.java:113)
 *     at com.sun.proxy.$Proxy2.getGreeting(Unknown Source)
 *     at com.uber.cadence.samples.hello.HelloException.main(HelloException.java:117)
 * Caused by: com.uber.cadence.workflow.ChildWorkflowFailureException: WorkflowType="GreetingChild::composeGreeting", ID="37ceb58c-9271-4fca-b5aa-ba06c5495214:1", RunID="47859b47-da4c-4225-876a-462421c98c72, EventID=10
 *     at java.lang.Thread.getStackTrace(Thread.java:1559)
 *     at com.uber.cadence.internal.dispatcher.ChildWorkflowInvocationHandler.executeChildWorkflow(ChildWorkflowInvocationHandler.java:114)
 *     at com.uber.cadence.internal.dispatcher.ChildWorkflowInvocationHandler.invoke(ChildWorkflowInvocationHandler.java:71)
 *     at com.sun.proxy.$Proxy5.composeGreeting(Unknown Source:0)
 *     at com.uber.cadence.samples.hello.HelloException$GreetingWorkflowImpl.getGreeting(HelloException.java:70)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method:0)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 *     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 *     at java.lang.reflect.Method.invoke(Method.java:498)
 *     at com.uber.cadence.internal.worker.POJOWorkflowImplementationFactory$POJOWorkflowImplementation.execute(POJOWorkflowImplementationFactory.java:160)
 * Caused by: com.uber.cadence.workflow.ActivityFailureException: ActivityType="GreetingActivities::composeGreeting" ActivityID="1", EventID=7
 *     at java.lang.Thread.getStackTrace(Thread.java:1559)
 *     at com.uber.cadence.internal.dispatcher.ActivityInvocationHandler.invoke(ActivityInvocationHandler.java:75)
 *     at com.sun.proxy.$Proxy6.composeGreeting(Unknown Source:0)
 *     at com.uber.cadence.samples.hello.HelloException$GreetingChildImpl.composeGreeting(HelloException.java:85)
 *     ... 5 more
 * Caused by: java.io.IOException: Hello World!
 *     at com.uber.cadence.samples.hello.HelloException$GreetingActivitiesImpl.composeGreeting(HelloException.java:93)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method:0)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 *     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 *     at java.lang.reflect.Method.invoke(Method.java:498)
 *     at com.uber.cadence.internal.worker.POJOActivityImplementationFactory$POJOActivityImplementation.execute(POJOActivityImplementationFactory.java:162)
 *
 * </pre>
 * Note that there is only one level of wrapping when crossing logical process boundaries. And that wrapper exception
 * adds a lot of relevant information about failure.
 * <p>
 * {@link IOException} is a checked exception. The standard Java way of adding <code>throws IOException</code> to
 * activity, child and workflow interfaces is not going to help. It is because at all levels it is never received directly, but
 * in wrapped form. Propagating it without wrapping would not allow adding additional context information
 * like activity, child workflow and parent workflow  types and IDs. The Cadence library solution is to provide special
 * wrapper method {@link Workflow#throwWrapped(Throwable)} which wraps a checked exception in a special runtime exception.
 * It is special because framework strips it when chaining exception across logical process boundaries. In this example
 * IOException is directly attached to ActivityFailureException besides being wrapped when rethrown.
 * </p>
 */
public class HelloException {

    private static final String TASK_LIST = "HelloChild";

    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);
    }

    /**
     * Activity interface is just a POJI
     */
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String greeting, String name);
    }

    public interface GreetingActivities {
        String composeGreeting(String greeting, String name);
    }

    /**
     * GreetingWorkflow implementation that calls GreetingsActivities#printIt.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {
            GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);
            return child.composeGreeting("Hello", name);
        }
    }

    /**
     * Child workflow implementation.
     * Workflow implementation must always be public for the Cadence to be able to create instances.
     */
    public static class GreetingChildImpl implements GreetingChild {
        private final GreetingActivities activities = Workflow.newActivityStub(
                GreetingActivities.class,
                new ActivityOptions.Builder().setScheduleToCloseTimeoutSeconds(10).build());

        @Override
        public String composeGreeting(String greeting, String name) {
            return activities.composeGreeting(greeting, name);
        }
    }

    private static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String greeting, String name) {
            try {
                throw new IOException(greeting + " " + name + "!");
            } catch (IOException e) {
                throw Workflow.throwWrapped(e);
            }
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);

        Worker worker = new Worker(DOMAIN, TASK_LIST);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        worker.start();

        WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
        WorkflowOptions workflowOptions = new WorkflowOptions.Builder()
                .setTaskList(TASK_LIST)
                .setExecutionStartToCloseTimeoutSeconds(30)
                .build();
        GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class,
                workflowOptions);
        try {
            workflow.getGreeting("World");
            throw new IllegalStateException("unreachable");
        } catch (WorkflowException e) {
            Throwable cause = Throwables.getRootCause(e);
            // prints "Hello World!"
            System.out.println(cause.getMessage());
            System.out.println("\nStack Trace:\n" + Throwables.getStackTraceAsString(e));
        }
        System.exit(0);
    }
}
