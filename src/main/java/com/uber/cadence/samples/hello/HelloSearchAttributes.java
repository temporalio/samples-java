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

import com.uber.cadence.DescribeWorkflowExecutionRequest;
import com.uber.cadence.DescribeWorkflowExecutionResponse;
import com.uber.cadence.SearchAttributes;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import java.nio.ByteBuffer;
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
    // Start a worker that hosts both workflow and activity implementations.
    Worker.Factory factory = new Worker.Factory(DOMAIN);
    Worker worker = factory.newWorker(TASK_LIST);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(HelloSearchAttributes.GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new HelloSearchAttributes.GreetingActivitiesImpl());
    // Start listening to the workflow and activity task lists.
    factory.start();

    // Start a workflow execution. Usually this is done from another program.
    WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);

    // Set search attributes in workflowOptions
    String workflowID = UUID.randomUUID().toString();
    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(TASK_LIST)
            .setWorkflowId(workflowID)
            .setSearchAttributes(generateSearchAttributes())
            .build();
    // Get a workflow stub using the same task list the worker uses.
    HelloSearchAttributes.GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(
            HelloSearchAttributes.GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("SearchAttributes");

    IWorkflowService cadenceService = new WorkflowServiceTChannel();
    WorkflowExecution execution = new WorkflowExecution();
    execution.setWorkflowId(workflowID);

    DescribeWorkflowExecutionRequest request = new DescribeWorkflowExecutionRequest();
    request.setDomain(DOMAIN);
    request.setExecution(execution);
    try {
      DescribeWorkflowExecutionResponse resp = cadenceService.DescribeWorkflowExecution(request);
      SearchAttributes searchAttributes = resp.workflowExecutionInfo.getSearchAttributes();
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
    Map<String, ByteBuffer> map = searchAttributes.getIndexedFields();
    ByteBuffer byteBuffer = map.get("CustomKeywordField");
    DataConverter dataConverter = JsonDataConverter.getInstance();
    final byte[] valueBytes = new byte[byteBuffer.limit() - byteBuffer.position()];
    byteBuffer.get(valueBytes, 0, valueBytes.length);
    return dataConverter.fromData(valueBytes, String.class, String.class);
  }
}
