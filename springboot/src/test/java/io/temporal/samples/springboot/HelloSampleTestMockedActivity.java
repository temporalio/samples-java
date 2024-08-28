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

package io.temporal.samples.springboot;

import static org.mockito.ArgumentMatchers.any;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.springboot.hello.HelloActivity;
import io.temporal.samples.springboot.hello.HelloActivityImpl;
import io.temporal.samples.springboot.hello.HelloWorkflow;
import io.temporal.samples.springboot.hello.model.Person;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest(classes = HelloSampleTestMockedActivity.Configuration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// set this to omit setting up embedded kafka
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@DirtiesContext
public class HelloSampleTestMockedActivity {

  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

  @Captor ArgumentCaptor<Person> personArgumentCaptor;

  @Autowired HelloActivity activity;

  @BeforeEach
  void setUp() {
    applicationContext.start();
  }

  @Test
  public void testHello() {
    HelloWorkflow workflow =
        workflowClient.newWorkflowStub(
            HelloWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("HelloSampleTaskQueue")
                .setWorkflowId("HelloSampleTest")
                .build());
    String result = workflow.sayHello(new Person("Temporal", "User"));
    Assert.notNull(result, "Greeting should not be null");
    Assert.isTrue(result.equals("Hello from mocked activity"), "Invalid result");

    Mockito.verify(activity, Mockito.times(1)).hello(personArgumentCaptor.capture());
    Assert.notNull(personArgumentCaptor.getValue(), "Invalid input");
    Assert.isTrue(
        personArgumentCaptor.getValue().getFirstName().equals("Temporal"),
        "Invalid person first name");
    Assert.isTrue(
        personArgumentCaptor.getValue().getLastName().equals("User"), "invalid person last name");
  }

  @ComponentScan
  public static class Configuration {
    @MockBean private HelloActivityImpl helloActivityMock;

    @Bean
    @Primary
    public HelloActivity getTestActivityImpl() {
      Mockito.when(helloActivityMock.hello(any())).thenReturn("Hello from mocked activity");
      return helloActivityMock;
    }
  }
}
