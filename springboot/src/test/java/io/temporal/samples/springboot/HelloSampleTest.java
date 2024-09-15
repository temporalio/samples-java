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

import io.temporal.api.enums.v1.TaskQueueKind;
import io.temporal.api.enums.v1.TaskQueueType;
import io.temporal.api.taskqueue.v1.TaskQueue;
import io.temporal.api.workflowservice.v1.DescribeTaskQueueRequest;
import io.temporal.api.workflowservice.v1.DescribeTaskQueueResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.springboot.hello.HelloWorkflow;
import io.temporal.samples.springboot.hello.model.Person;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest(classes = HelloSampleTest.Configuration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// set this to omit setting up embedded kafka
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@DirtiesContext
public class HelloSampleTest {

  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

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
    Assert.isTrue(result.equals("Hello Temporal User!"), "Invalid result");
  }

  @Test
  public void testGetQueueStatus() {

    Assert.notNull(workflowClient, "workflowClient should not be null");

    TaskQueue tq =
        TaskQueue.newBuilder()
            .setKind(TaskQueueKind.TASK_QUEUE_KIND_UNSPECIFIED)
            .setName("HelloSampleTaskQueue")
            .build();

    DescribeTaskQueueRequest taskQrqst =
        DescribeTaskQueueRequest.newBuilder()
            .setNamespace("default")
            .setTaskQueue(tq)
            .setIncludeTaskQueueStatus(true)
            .setTaskQueueType(TaskQueueType.TASK_QUEUE_TYPE_ACTIVITY)
            .build();

    WorkflowServiceStubsOptions serviceStubOptions =
        WorkflowServiceStubsOptions.newBuilder().setTarget("localhost:7233").build();

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newConnectedServiceStubs(serviceStubOptions, null);

    // below describeTaskQueue is having issue and is throwing io.grpc.StatusRuntimeException:
    // RESOURCE_EXHAUSTED: namespace rate limit exceeded
    // since temporal server 1.24.2 and 1.25.0 when concurrent requests to describeTaskQueue are
    // sent
    // same code runs ok with temporal server 1.22.2
    DescribeTaskQueueResponse activityResponse =
        service.blockingStub().describeTaskQueue(taskQrqst);
    // System.out.println(activityResponse.getPollersCount());

    Assert.isTrue(activityResponse.getPollersCount() > 0, "activity Poller count must be > 0");

    taskQrqst =
        DescribeTaskQueueRequest.newBuilder(taskQrqst)
            .setTaskQueueType(TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW)
            .build();

    // below describeTaskQueue is having issue and is throwing io.grpc.StatusRuntimeException:
    // RESOURCE_EXHAUSTED: namespace rate limit exceeded
    // since temporal server 1.24.2 and 1.25.0 when concurrent requests to describeTaskQueue are
    // sent
    // same code runs ok with temporal server 1.22.2
    DescribeTaskQueueResponse wfResponse = service.blockingStub().describeTaskQueue(taskQrqst);
    // System.out.println(wfResponse.getPollersCount());
    Assert.isTrue(wfResponse.getPollersCount() > 0, "workflow Poller count must be > 0");
  }

  /**
   * This test succeeds when temporal server 1.22.2 is run but fails since temporal server 1.24.2 or
   * 1.25.0 and starts throwing io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: namespace rate
   * limit exceeded *
   */
  @Test
  public void testWithConcurrency() throws InterruptedException {
    int numberOfThreads = 10;
    ExecutorService service = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(
          () -> {
            testGetQueueStatus();
            latch.countDown();
          });
    }
    // latch.await();
  }

  @ComponentScan
  public static class Configuration {}
}
