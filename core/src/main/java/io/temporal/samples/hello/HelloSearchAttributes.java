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
import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributeUpdate;
import io.temporal.common.SearchAttributes;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.GlobalDataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;

/**
 * Sample Temporal workflow that demonstrates setting up and retrieving workflow search attributes.
 */
public class HelloSearchAttributes {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloSearchAttributesTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloSearchAttributesWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
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
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
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

    private Logger logger = Workflow.getLogger(this.getClass().getName());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      Workflow.upsertTypedSearchAttributes(
          SearchAttributeUpdate.valueSet(
              SearchAttributeKey.forKeyword("CustomKeywordField"), "key2"));

      OffsetDateTime offsetSA =
          Workflow.getTypedSearchAttributes()
              .get(SearchAttributeKey.forOffsetDateTime("CustomDatetimeField"));
      logger.info("From workflow - OffsetSA: " + offsetSA);

      return activities.composeGreeting("Hello", name);
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
    worker.registerWorkflowImplementationTypes(HelloSearchAttributes.GreetingWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new HelloSearchAttributes.GreetingActivitiesImpl());

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
            .setTypedSearchAttributes(
                SearchAttributes.newBuilder()
                    .set(SearchAttributeKey.forKeyword("CustomKeywordField"), "key")
                    .set(SearchAttributeKey.forLong("CustomIntField"), Long.valueOf(1L))
                    .set(SearchAttributeKey.forDouble("CustomDoubleField"), Double.valueOf(1.0))
                    .set(SearchAttributeKey.forBoolean("CustomBoolField"), true)
                    .set(
                        SearchAttributeKey.forOffsetDateTime("CustomDatetimeField"),
                        OffsetDateTime.now(ZoneId.of("America/Los_Angeles")))
                    .set(SearchAttributeKey.forText("CustomStringField"), "text")
                    .build())
            .build();

    // Create the workflow client stub. It is used to start our workflow execution.
    HelloSearchAttributes.GreetingWorkflow workflow =
        client.newWorkflowStub(HelloSearchAttributes.GreetingWorkflow.class, workflowOptions);

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("SearchAttributes");

    // Get the workflow execution for the same id as our defined workflow
    WorkflowExecution execution = WorkflowExecution.newBuilder().setWorkflowId(WORKFLOW_ID).build();

    // Create the DescribeWorkflowExecutionRequest through which we can query our client for our
    // search queries
    DescribeWorkflowExecutionRequest request =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setExecution(execution)
            .build();

    try {
      // Get the DescribeWorkflowExecutionResponse from our service
      DescribeWorkflowExecutionResponse resp =
          service.blockingStub().describeWorkflowExecution(request);

      // get all search attributes
      io.temporal.api.common.v1.SearchAttributes sa =
          resp.getWorkflowExecutionInfo().getSearchAttributes();
      // Get the specific value of a keyword from the payload.
      // In this case it is the "CustomKeywordField" with the value of "keys"
      // You can update the code to extract other defined search attribute as well
      String keyword = getKeywordFromSearchAttribute(sa, "CustomKeywordField");
      // Print the value of the "CustomKeywordField" field
      System.out.printf("CustomKeywordField search attribute values: %s\n", keyword);
    } catch (Exception e) {
      System.out.println(e);
    }

    // Print the workflow execution results
    System.out.println(greeting);
    System.exit(0);
  }

  // example for extracting a value from search attributes
  static String getKeywordFromSearchAttribute(
      io.temporal.api.common.v1.SearchAttributes searchAttributes, String key) {
    Payload field = searchAttributes.getIndexedFieldsOrThrow(key);
    DataConverter dataConverter = GlobalDataConverter.get();
    return dataConverter.fromPayload(field, String.class, String.class);
  }
}
