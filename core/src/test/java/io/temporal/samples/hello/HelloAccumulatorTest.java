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

import static io.temporal.samples.hello.HelloAccumulator.MAX_AWAIT_TIME;

import io.temporal.client.BatchRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloAccumulator.Greeting;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashSet;
import org.junit.Rule;
import org.junit.Test;

public class HelloAccumulatorTest {

  private TestWorkflowEnvironment testEnv;

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloAccumulator.AccumulatorWorkflowImpl.class)
          .setActivityImplementations(new HelloAccumulator.GreetingActivitiesImpl())
          .build();

  @Test
  public void testWorkflow() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    HelloAccumulator.AccumulatorWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloAccumulator.AccumulatorWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowClient.start(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    Greeting xvxGreeting = new Greeting("XVX Robot", bucket, "1123581321");

    workflow.sendGreeting(xvxGreeting);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (1)");
    assert results.contains("XVX Robot");
  }

  @Test
  public void testJustExit() {
    String bucket = "blue";
    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    HelloAccumulator.AccumulatorWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloAccumulator.AccumulatorWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowClient.start(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    workflow.exit();

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (0)");
    assert !results.contains("Robot");
  }

  @Test
  public void testNoExit() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    HelloAccumulator.AccumulatorWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloAccumulator.AccumulatorWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowClient.start(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    Greeting xvxGreeting = new Greeting("XVX Robot", bucket, "1123581321");

    workflow.sendGreeting(xvxGreeting);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (1)");
    assert results.contains("XVX Robot");
  }

  @Test
  public void testMultipleGreetings() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    HelloAccumulator.AccumulatorWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloAccumulator.AccumulatorWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowClient.start(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    workflow.sendGreeting(new Greeting("XVX Robot", bucket, "1123581321"));
    workflow.sendGreeting(new Greeting("Han Robot", bucket, "112358"));

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (2)");
    assert results.contains("XVX Robot");
    assert results.contains("Han Robot");
  }

  @Test
  public void testDuplicateGreetings() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    HelloAccumulator.AccumulatorWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloAccumulator.AccumulatorWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowClient.start(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    workflow.sendGreeting(new Greeting("XVX Robot", bucket, "1123581321"));
    workflow.sendGreeting(new Greeting("Han Robot", bucket, "1123581321"));

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (1)");
    assert results.contains("XVX Robot");
    assert !results.contains("Han Robot");
  }

  @Test
  public void testWrongBucketGreeting() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    HelloAccumulator.AccumulatorWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloAccumulator.AccumulatorWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowClient.start(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    workflow.sendGreeting(new Greeting("Bad Robot", "orange", "1123581321"));
    workflow.sendGreeting(new Greeting("XVX Robot", bucket, "11235"));

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (1)");
    assert results.contains("XVX Robot");
    assert !results.contains("Bad Robot");
  }

  @Test
  public void testSignalWithStart() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    WorkflowClient client = testWorkflowRule.getWorkflowClient();
    HelloAccumulator.AccumulatorWorkflow workflow =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    Greeting starterGreeting = new Greeting("Robby Robot", bucket, "112");
    BatchRequest request = client.newSignalWithStartRequest();
    request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow::sendGreeting, starterGreeting);
    client.signalWithStart(request);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (1)");
    assert results.contains("Robby Robot");
  }

  @Test
  public void testWaitTooLongForFirstWorkflow() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    WorkflowClient client = testWorkflowRule.getWorkflowClient();
    HelloAccumulator.AccumulatorWorkflow workflow =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting starterGreeting = new Greeting("Robby Robot", bucket, "112");
    BatchRequest request = client.newSignalWithStartRequest();
    request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow::sendGreeting, starterGreeting);
    client.signalWithStart(request);

    // testEnv.sleep(MAX_AWAIT_TIME.plus(Duration.ofMillis(1))); is not long enough
    // to guarantee the
    // first workflow will end
    testEnv.sleep(MAX_AWAIT_TIME.plus(Duration.ofMillis(100)));

    HelloAccumulator.AccumulatorWorkflow workflow2 =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting secondGreeting = new Greeting("Dave Robot", bucket, "1123");

    request = client.newSignalWithStartRequest();
    request.add(workflow2::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow2::sendGreeting, secondGreeting);
    client.signalWithStart(request);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (1)");
    assert !results.contains("Robby Robot");
    assert results.contains("Dave Robot");
  }

  @Test
  public void testWaitNotLongEnoughForNewWorkflow() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    WorkflowClient client = testWorkflowRule.getWorkflowClient();
    HelloAccumulator.AccumulatorWorkflow workflow =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting starterGreeting = new Greeting("Robby Robot", bucket, "112");
    BatchRequest request = client.newSignalWithStartRequest();
    request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow::sendGreeting, starterGreeting);
    client.signalWithStart(request);

    testEnv.sleep(MAX_AWAIT_TIME.minus(Duration.ofMillis(1)));

    HelloAccumulator.AccumulatorWorkflow workflow2 =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting secondGreeting = new Greeting("Dave Robot", bucket, "1123");

    request = client.newSignalWithStartRequest();
    request.add(workflow2::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow2::sendGreeting, secondGreeting);
    client.signalWithStart(request);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (2)");
    assert results.contains("Robby Robot");
    assert results.contains("Dave Robot");
  }

  @Test
  public void testWaitExactlyMAX_TIME() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    WorkflowClient client = testWorkflowRule.getWorkflowClient();
    HelloAccumulator.AccumulatorWorkflow workflow =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting starterGreeting = new Greeting("Robby Robot", bucket, "112");
    BatchRequest request = client.newSignalWithStartRequest();
    request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow::sendGreeting, starterGreeting);
    client.signalWithStart(request);

    testEnv.sleep(MAX_AWAIT_TIME);

    HelloAccumulator.AccumulatorWorkflow workflow2 =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting secondGreeting = new Greeting("Dave Robot", bucket, "1123");

    request = client.newSignalWithStartRequest();
    request.add(workflow2::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow2::sendGreeting, secondGreeting);
    client.signalWithStart(request);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (2)");
    assert results.contains("Robby Robot");
    assert results.contains("Dave Robot");
  }

  @Test
  public void testSignalAfterExit() {
    String bucket = "blue";

    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    testEnv = testWorkflowRule.getTestEnvironment();
    testEnv.start();

    WorkflowClient client = testWorkflowRule.getWorkflowClient();
    HelloAccumulator.AccumulatorWorkflow workflow =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting starterGreeting = new Greeting("Robby Robot", bucket, "112");
    BatchRequest request = client.newSignalWithStartRequest();
    request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow::sendGreeting, starterGreeting);
    client.signalWithStart(request);

    HelloAccumulator.AccumulatorWorkflow workflow2 =
        client.newWorkflowStub(
            HelloAccumulator.AccumulatorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(bucket)
                .setWorkflowId("helloacc-blue")
                .build());

    Greeting secondGreeting = new Greeting("Dave Robot", bucket, "1123");

    request = client.newSignalWithStartRequest();
    request.add(workflow2::accumulateGreetings, bucket, greetingList, allGreetingsSet);
    request.add(workflow2::sendGreeting, secondGreeting);

    // exit signal the workflow we signaled-to-start
    workflow.exit();

    // try to signal with start the workflow
    client.signalWithStart(request);

    String results = workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);
    assert results.contains("Hello (2)");
    assert results.contains("Robby Robot");
    assert results.contains("Dave Robot");
  }
}
