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

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityOptions;
import io.temporal.activity.DynamicActivity;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.converter.EncodedValues;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloDynamic {
  // Define the task queue name
  public static final String TASK_QUEUE = "HelloDynamicTaskQueue";

  // Define our workflow unique id
  public static final String WORKFLOW_ID = "HelloDynamicWorkflow";

  // Dynamic Workflow Implementation
  public static class DynamicGreetingWorkflowImpl implements DynamicWorkflow {
    private String name;

    @Override
    public Object execute(EncodedValues args) {
      String greeting = args.get(0, String.class);
      String type = Workflow.getInfo().getWorkflowType();

      // Register dynamic signal handler
      Workflow.registerListener(
          (DynamicSignalHandler)
              (signalName, encodedArgs) -> name = encodedArgs.get(0, String.class));

      // Define activity options and get ActivityStub
      ActivityStub activity =
          Workflow.newUntypedActivityStub(
              ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());
      // Execute the dynamic Activity. Note that the provided Activity name is not
      // explicitly registered with the Worker
      String greetingResult = activity.execute("DynamicACT", String.class, greeting, name, type);

      // Return results
      return greetingResult;
    }
  }

  // Dynamic Activity implementation
  public static class DynamicGreetingActivityImpl implements DynamicActivity {
    @Override
    public Object execute(EncodedValues args) {
      String activityType = Activity.getExecutionContext().getInfo().getActivityType();
      return activityType
          + ": "
          + args.get(0, String.class)
          + " "
          + args.get(1, String.class)
          + " from: "
          + args.get(2, String.class);
    }
  }

  /**
   * With our dynamic Workflow and Activities defined, we can now start execution. The main method
   * starts the worker and then the workflow.
   */
  public static void main(String[] arg) {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow
     * Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our dynamic workflow implementation with the worker. Workflow implementations must
     * be known to the worker at runtime in order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(DynamicGreetingWorkflowImpl.class);

    /*
     * Register our dynamic workflow activity implementation with the worker. Since workflow
     * activities are stateless and thread-safe, we need to register a shared instance.
     */
    worker.registerActivitiesImplementations(new DynamicGreetingActivityImpl());

    /*
     * Start all the Workers that are in this process. The Workers will then start polling for
     * Workflow Tasks and Activity Tasks.
     */
    factory.start();

    /*
     * Create the workflow stub Note that the Workflow type is not explicitly registered with the
     * Worker
     */
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub("DynamicWF", workflowOptions);

    // Start workflow execution and signal right after Pass in the workflow args and signal args
    workflow.signalWithStart("greetingSignal", new Object[] {"John"}, new Object[] {"Hello"});

    // Wait for workflow to finish getting the results
    String result = workflow.getResult(String.class);

    System.out.println(result);

    System.exit(0);
  }
}
