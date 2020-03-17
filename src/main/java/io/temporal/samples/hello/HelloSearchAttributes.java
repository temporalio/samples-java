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

import com.google.protobuf.ByteString;
import io.temporal.activity.ActivityMethod;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.converter.DataConverter;
import io.temporal.converter.JsonDataConverter;
import io.temporal.proto.common.SearchAttributes;
import io.temporal.proto.common.WorkflowExecution;
import io.temporal.proto.workflowservice.DescribeWorkflowExecutionRequest;
import io.temporal.proto.workflowservice.DescribeWorkflowExecutionResponse;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowMethod;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HelloSearchAttributes {

  static final String TASK_LIST = "HelloSearchAttributes";

  /** Workflow interface has to have at least one method annotated with @WorkflowMethod. */
  public interface GreetingWorkflow {
    /** @return greeting string */
    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 10, taskList = TASK_LIST)
    String getGreeting(String name);
  }

  /** Activity interface is just a POJI. */
  public interface GreetingActivities {
    @ActivityMethod(scheduleToCloseTimeoutSeconds = 2)
    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#composeGreeting. */
  public static class GreetingWorkflowImpl implements HelloActivity.GreetingWorkflow {

    /**
     * Activity stub implements activity interface and proxies calls to it to Cadence activity
     * invocations. Because activities are reentrant, only a single stub can be used for multiple
     * activity invocations.
     */
    private final HelloActivity.GreetingActivities activities =
        Workflow.newActivityStub(HelloActivity.GreetingActivities.class);

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  static class GreetingActivitiesImpl implements HelloActivity.GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + "!";
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newInstance(WorkflowServiceStubs.LOCAL_DOCKER_TARGET);
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task lists
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task list and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_LIST);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(HelloSearchAttributes.GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new HelloSearchAttributes.GreetingActivitiesImpl());
    // Start listening to the workflow and activity task lists.
    factory.start();

    // Set search attributes in workflowOptions
    String workflowID = UUID.randomUUID().toString();
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskList(TASK_LIST)
            .setWorkflowId(workflowID)
            .setSearchAttributes(generateSearchAttributes())
            .build();
    // Get a workflow stub using the same task list the worker uses.
    HelloSearchAttributes.GreetingWorkflow workflow =
        client.newWorkflowStub(HelloSearchAttributes.GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("SearchAttributes");

    WorkflowExecution execution = WorkflowExecution.newBuilder().setWorkflowId(workflowID).build();

    DescribeWorkflowExecutionRequest request =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setDomain(client.getDomain())
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

  // CustomDatetimeField takes string like "2018-07-14T17:45:55.9483536" or
  // "2019-01-01T00:00:00-08:00" as value
  private static String generateDateTimeFieldValue() {
    LocalDateTime currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    return currentDateTime.format(formatter);
  }

  // example for extract value from search attributes
  private static String getKeywordFromSearchAttribute(SearchAttributes searchAttributes) {
    ByteString field = searchAttributes.getIndexedFieldsOrThrow("CustomKeywordField");
    DataConverter dataConverter = JsonDataConverter.getInstance();
    return dataConverter.fromData(field.toByteArray(), String.class, String.class);
  }
}
