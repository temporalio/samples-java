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

package io.temporal.samples.hello;

import com.google.common.base.Throwables;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowMethod;
import java.io.IOException;
import java.time.Duration;

/**
 * Demonstrates exception propagation across activity, child workflow and workflow client
 * boundaries. Shows how to deal with checked exceptions.
 * <li>
 *
 *     <ul>
 *       Exceptions thrown by an activity are received by the workflow wrapped into an {@link
 *       io.temporal.workflow.ActivityFailureException}.
 * </ul>
 *
 * <ul>
 *   Exceptions thrown by a child workflow are received by a parent workflow wrapped into a {@link
 *   io.temporal.workflow.ChildWorkflowFailureException}.
 * </ul>
 *
 * <ul>
 *   Exceptions thrown by a workflow are received by a workflow client wrapped into {@link
 *   io.temporal.client.WorkflowFailureException}.
 * </ul>
 *
 * <p>In this example a Workflow Client executes a workflow which executes a child workflow which
 * executes an activity which throws an IOException. The resulting exception stack trace is:
 *
 * <pre>
 * io.temporal.client.WorkflowFailureException: WorkflowType="GreetingWorkflow::getGreeting", WorkflowID="38b9ce7a-e370-4cd8-a9f3-35e7295f7b3d", RunID="37ceb58c-9271-4fca-b5aa-ba06c5495214
 *     at io.temporal.internal.dispatcher.UntypedWorkflowStubImpl.getResult(UntypedWorkflowStubImpl.java:139)
 *     at io.temporal.internal.dispatcher.UntypedWorkflowStubImpl.getResult(UntypedWorkflowStubImpl.java:111)
 *     at io.temporal.internal.dispatcher.WorkflowExternalInvocationHandler.startWorkflow(WorkflowExternalInvocationHandler.java:187)
 *     at io.temporal.internal.dispatcher.WorkflowExternalInvocationHandler.invoke(WorkflowExternalInvocationHandler.java:113)
 *     at com.sun.proxy.$Proxy2.getGreeting(Unknown Source)
 *     at io.temporal.samples.hello.HelloException.main(HelloException.java:117)
 * Caused by: io.temporal.workflow.ChildWorkflowFailureException: WorkflowType="GreetingChild::composeGreeting", ID="37ceb58c-9271-4fca-b5aa-ba06c5495214:1", RunID="47859b47-da4c-4225-876a-462421c98c72, EventID=10
 *     at java.lang.Thread.getStackTrace(Thread.java:1559)
 *     at io.temporal.internal.dispatcher.ChildWorkflowInvocationHandler.executeChildWorkflow(ChildWorkflowInvocationHandler.java:114)
 *     at io.temporal.internal.dispatcher.ChildWorkflowInvocationHandler.invoke(ChildWorkflowInvocationHandler.java:71)
 *     at com.sun.proxy.$Proxy5.composeGreeting(Unknown Source:0)
 *     at io.temporal.samples.hello.HelloException$GreetingWorkflowImpl.getGreeting(HelloException.java:70)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method:0)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 *     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 *     at java.lang.reflect.Method.invoke(Method.java:498)
 *     at io.temporal.internal.worker.POJOWorkflowImplementationFactory$POJOWorkflowImplementation.execute(POJOWorkflowImplementationFactory.java:160)
 * Caused by: io.temporal.workflow.ActivityFailureException: ActivityType="GreetingActivities::composeGreeting" ActivityID="1", EventID=7
 *     at java.lang.Thread.getStackTrace(Thread.java:1559)
 *     at io.temporal.internal.dispatcher.ActivityInvocationHandler.invoke(ActivityInvocationHandler.java:75)
 *     at com.sun.proxy.$Proxy6.composeGreeting(Unknown Source:0)
 *     at io.temporal.samples.hello.HelloException$GreetingChildImpl.composeGreeting(HelloException.java:85)
 *     ... 5 more
 * Caused by: java.io.IOException: Hello World!
 *     at io.temporal.samples.hello.HelloException$GreetingActivitiesImpl.composeGreeting(HelloException.java:93)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method:0)
 *     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 *     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 *     at java.lang.reflect.Method.invoke(Method.java:498)
 *     at io.temporal.internal.worker.POJOActivityImplementationFactory$POJOActivityImplementation.execute(POJOActivityImplementationFactory.java:162)
 *
 * </pre>
 *
 * Note that there is only one level of wrapping when crossing logical process boundaries. And that
 * wrapper exception adds a lot of relevant information about failure.
 *
 * <p>{@link IOException} is a checked exception. The standard Java way of adding <code>
 * throws IOException</code> to activity, child and workflow interfaces is not going to help. It is
 * because at all levels it is never received directly, but in wrapped form. Propagating it without
 * wrapping would not allow adding additional context information like activity, child workflow and
 * parent workflow types and IDs. The Temporal library solution is to provide a special wrapper
 * method {@link Workflow#wrap(Exception)} which wraps a checked exception in a special runtime
 * exception. It is special because the framework strips it when chaining exceptions across logical
 * process boundaries. In this example IOException is directly attached to ActivityFailureException
 * besides being wrapped when rethrown.
 */
public class HelloException {

  static final String TASK_LIST = "HelloException";

  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreeting(String name);
  }

  public interface GreetingChild {
    @WorkflowMethod
    String composeGreeting(String greeting, String name);
  }

  public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
  }

  /** Parent implementation that calls GreetingChild#composeGreeting. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    @Override
    public String getGreeting(String name) {
      GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);
      return child.composeGreeting("Hello", name);
    }
  }

  /** Child workflow implementation. */
  public static class GreetingChildImpl implements GreetingChild {
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public String composeGreeting(String greeting, String name) {
      return activities.composeGreeting(greeting, name);
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
      try {
        throw new IOException(greeting + " " + name + "!");
      } catch (IOException e) {
        // Wrapping the exception as checked exceptions in activity and workflow interface methods
        // are prohibited.
        // It will be unwrapped and attached as a cause to the ActivityFailureException.
        throw Workflow.wrap(e);
      }
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task lists
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task list and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    factory.start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      Throwable cause = Throwables.getRootCause(e);
      // prints "Hello World!"
      System.out.println(cause.getMessage());
      // Look at the stack trace which includes the complete information about the location of the
      // failure.
      System.out.println("\nStack Trace:\n" + Throwables.getStackTraceAsString(e));
    }
    System.exit(0);
  }
}
