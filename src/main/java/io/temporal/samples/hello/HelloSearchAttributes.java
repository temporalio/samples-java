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
import io.temporal.api.common.v1.SearchAttributes;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TODO(maxim): This sample is broken. https://github.com/temporalio/temporal-java-samples/issues/10
 */
public class HelloSearchAttributes {

  static final String TASK_QUEUE = "HelloSearchAttributes";

  /** Workflow interface has to have at least one method annotated with @WorkflowMethod. */
  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** Activity interface is just a POJI. */
  @ActivityInterface
  public interface GreetingActivities {
    @ActivityMethod
    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#composeGreeting. */
  public static class GreetingWorkflowImpl implements HelloActivity.GreetingWorkflow {

    /**
     * Activity stub implements activity interface and proxies calls to it to Temporal activity
     * invocations. Because activities are reentrant, only a single stub can be used for multiple
     * activity invocations.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + "!";
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(HelloSearchAttributes.GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new HelloSearchAttributes.GreetingActivitiesImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Set search attributes in workflowOptions
    String workflowID = UUID.randomUUID().toString();
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(workflowID)
            .setSearchAttributes(generateSearchAttributes())
            .build();
    // Get a workflow stub using the same task queue the worker uses.
    HelloSearchAttributes.GreetingWorkflow workflow =
        client.newWorkflowStub(HelloSearchAttributes.GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("SearchAttributes");

    WorkflowExecution execution = WorkflowExecution.newBuilder().setWorkflowId(workflowID).build();

    DescribeWorkflowExecutionRequest request =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setExecution(execution)
            .build();
    try {
      DescribeWorkflowExecutionResponse resp =
          service.blockingStub().describeWorkflowExecution(request);
      SearchAttributes searchAttributes = resp.getWorkflowExecutionInfo().getSearchAttributes();
      String keyword = getKeywordFromSearchAttribute(searchAttributes);
      System.out.printf("In workflow we get CustomKeywordField is: %s\n", keyword);
    } catch (Exception e) {
      System.out.println(e);
    }

    System.out.println(greeting);
    System.exit(0);
  }

  private static Map<String, Object> generateSearchAttributes() {
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put(
        "CustomKeywordField",
        "keys"); // each field can also be array such as: String[] keys = {"k1", "k2"};
    searchAttributes.put("CustomIntField", 1);
    searchAttributes.put("CustomDoubleField", 0.1);
    searchAttributes.put("CustomBoolField", true);
    searchAttributes.put("CustomDatetimeField", generateDateTimeFieldValue());
    searchAttributes.put(
        "CustomStringField",
        "String field is for text. When query, it will be tokenized for partial match. StringTypeField cannot be used in Order By");
    return searchAttributes;
  }

  // CustomDatetimeField takes times encoded in the  RFC 3339 format.
  private static String generateDateTimeFieldValue() {
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        .format(LocalDateTime.now(ZoneId.systemDefault()));
  }

  // example for extract value from search attributes
  private static String getKeywordFromSearchAttribute(SearchAttributes searchAttributes) {
    Payload field = searchAttributes.getIndexedFieldsOrThrow("CustomKeywordField");
    DataConverter dataConverter = DataConverter.getDefaultInstance();
    return dataConverter.fromPayload(field, String.class, String.class);
  }
}
