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

package io.temporal.samples.payloadconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.samples.payloadconverter.cloudevents.CEWorkflow;
import io.temporal.samples.payloadconverter.cloudevents.CEWorkflowImpl;
import io.temporal.samples.payloadconverter.cloudevents.CloudEventsPayloadConverter;
import io.temporal.testing.TestWorkflowRule;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class CloudEventsPayloadConverterTest {

  private DefaultDataConverter ddc =
      DefaultDataConverter.newDefaultInstance()
          .withPayloadConverterOverrides(new CloudEventsPayloadConverter());

  private WorkflowClientOptions workflowClientOptions =
      WorkflowClientOptions.newBuilder().setDataConverter(ddc).build();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowClientOptions(workflowClientOptions)
          .setWorkflowTypes(CEWorkflowImpl.class)
          .build();

  @Test
  public void testActivityImpl() {
    List<CloudEvent> cloudEventList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      cloudEventList.add(
          CloudEventBuilder.v1()
              .withId(String.valueOf(100 + i))
              .withType("example.demo")
              .withSource(URI.create("http://temporal.io"))
              .withData(
                  "application/json",
                  ("{\n" + "\"greeting\": \"hello " + i + "\"\n" + "}")
                      .getBytes(Charset.defaultCharset()))
              .build());
    }

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    CEWorkflow workflow =
        testWorkflowRule.getWorkflowClient().newWorkflowStub(CEWorkflow.class, workflowOptions);
    // start async
    WorkflowClient.start(workflow::exec, cloudEventList.get(0));

    for (int j = 1; j < 10; j++) {
      workflow.addEvent(cloudEventList.get(j));
    }

    // Get the CE result and get its data (JSON)
    String result =
        ((JsonCloudEventData) workflow.getLastEvent().getData()).getNode().get("greeting").asText();

    assertNotNull(result);
    assertEquals("hello 9", result);
  }
}
