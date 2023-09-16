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

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Sample Temporal workflow that demonstrates setting up, updating, and retrieving workflow search
 * attributes using the typed search attributes API.
 */
public class HelloTypedSearchAttributes {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloTypedSearchAttributesTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloTypedSearchAttributesWorkflow";

  // Define all our search attributes with appropriate types
  static final SearchAttributeKey<String> CUSTOM_KEYWORD_SA =
      SearchAttributeKey.forKeyword("CustomKeywordField");
  static final SearchAttributeKey<Long> CUSTOM_LONG_SA =
      SearchAttributeKey.forLong("CustomIntField");
  static final SearchAttributeKey<Double> CUSTOM_DOUBLE_SA =
      SearchAttributeKey.forDouble("CustomDoubleField");
  static final SearchAttributeKey<Boolean> CUSTOM_BOOL_SA =
      SearchAttributeKey.forBoolean("CustomBoolField");
  static final SearchAttributeKey<OffsetDateTime> CUSTOM_OFFSET_DATE_TIME_SA =
      SearchAttributeKey.forOffsetDateTime("CustomDatetimeField");
  static final SearchAttributeKey<String> CUSTOM_STRING_SA =
      SearchAttributeKey.forText("CustomStringField");

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see WorkflowInterface
   * @see WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see ActivityInterface
   * @see ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {
    @ActivityMethod
    String composeGreeting(String greeting, String name);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Define the GreetingActivities stub. Activity stubs implement activity interfaces and proxy
     * calls to it to Temporal activity invocations. Since Temporal activities are reentrant, a
     * single activity stub can be used for multiple activity invocations.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 2 seconds.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String getGreeting(String name) {
      // Show how to update typed search attributes inside a workflow. The first parameter shows how
      // to remove a search attribute. The second parameter shows how to update a value.
      Workflow.upsertTypedSearchAttributes(
          CUSTOM_LONG_SA.valueUnset(), CUSTOM_KEYWORD_SA.valueSet("Hello"));
      // Get the search attributes currently set on this workflow
      io.temporal.common.SearchAttributes searchAttributes = Workflow.getTypedSearchAttributes();
      // Get a particular value out of the container using the typed key
      String greeting = searchAttributes.get(CUSTOM_KEYWORD_SA);
      // This is a blocking call that returns only after the activity has completed.
      return activities.composeGreeting(greeting, name);
    }
  }

  /**
   * Implementation of our workflow activity interface. It overwrites our defined composeGreeting
   * activity method.
   */
  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + "!";
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    // Define the workflow service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
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
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(
        HelloTypedSearchAttributes.GreetingWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(
        new HelloTypedSearchAttributes.GreetingActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Set our workflow options.
    // Note that we set our search attributes here
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId(WORKFLOW_ID)
            .setTaskQueue(TASK_QUEUE)
            .setTypedSearchAttributes(generateTypedSearchAttributes())
            .build();

    // Create the workflow client stub. It is used to start our workflow execution.
    HelloTypedSearchAttributes.GreetingWorkflow workflow =
        client.newWorkflowStub(HelloTypedSearchAttributes.GreetingWorkflow.class, workflowOptions);

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("TypedSearchAttributes");

    // Print the workflow execution results
    System.out.println(greeting);
    System.exit(0);
  }

  // Generate our example search option
  private static io.temporal.common.SearchAttributes generateTypedSearchAttributes() {
    return io.temporal.common.SearchAttributes.newBuilder()
        .set(CUSTOM_KEYWORD_SA, "keyword")
        .set(CUSTOM_LONG_SA, 1l)
        .set(CUSTOM_DOUBLE_SA, 0.1)
        .set(CUSTOM_BOOL_SA, true)
        .set(CUSTOM_OFFSET_DATE_TIME_SA, OffsetDateTime.now(ZoneOffset.UTC))
        .set(
            CUSTOM_STRING_SA,
            "String field is for text. When query, it will be tokenized for partial match. StringTypeField cannot be used in Order By")
        .build();
  }
}
