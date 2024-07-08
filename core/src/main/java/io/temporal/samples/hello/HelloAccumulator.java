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
import io.temporal.activity.ActivityOptions;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.BatchRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Sample Temporal Workflow Definition that accumulates events.
 * This sample implements the Accumulator Pattern: collect many meaningful
 *  things that need to be collected and worked on together, such as
 *  all payments for an account, or all account updates by account.
 *
 * This sample models robots being created throughout the time period,
 *  groups them by what color they are, and greets all the robots
 *  of a color at the end.
 *
 * A new workflow is created per grouping. Workflows continue as new as needed.
 *  A sample activity at the end is given, and you could add an activity to
 *  process individual events in the processGreeting() method.
 */
public class HelloAccumulator {
  // set a time to wait for another signal to come in, e.g.
  // Duration.ofDays(30);
  static final Duration MAX_AWAIT_TIME = Duration.ofMinutes(1);

  static final String TASK_QUEUE = "HelloAccumulatorTaskQueue";
  static final String WORKFLOW_ID_PREFIX = "HelloAccumulatorWorkflow";

  public static class Greeting implements Serializable {
    String greetingText;
    String bucket;
    String greetingKey;

    public String getGreetingText() {
      return greetingText;
    }

    public void setGreetingText(String greetingText) {
      this.greetingText = greetingText;
    }

    public String getBucket() {
      return bucket;
    }

    public void setBucket(String bucket) {
      this.bucket = bucket;
    }

    public String getGreetingKey() {
      return greetingKey;
    }

    public void setGreetingKey(String greetingKey) {
      this.greetingKey = greetingKey;
    }

    public Greeting(String greetingText, String bucket, String greetingKey) {
      this.greetingText = greetingText;
      this.bucket = bucket;
      this.greetingKey = greetingKey;
    }

    public Greeting() {}

    @Override
    public String toString() {
      return "Greeting [greetingText="
          + greetingText
          + ", bucket="
          + bucket
          + ", greetingKey="
          + greetingKey
          + "]";
    }
  }

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
  public interface AccumulatorWorkflow {
    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String accumulateGreetings(
        String bucketKey, Deque<Greeting> greetings, Set<String> allGreetingsSet);

    // Define the workflow sendGreeting signal method. This method is executed when
    // the workflow receives a greeting signal.
    @SignalMethod
    void sendGreeting(Greeting greeting);

    // Define the workflow exit signal method. This method is executed when the
    // workflow receives an exit signal.
    @SignalMethod
    void exit();
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
    String composeGreeting(Deque<Greeting> greetings);
  }

  /** Simple activity implementation. */
  static class GreetingActivitiesImpl implements GreetingActivities {

    // here is where we process all of the signals together
    @Override
    public String composeGreeting(Deque<Greeting> greetings) {
      List<String> greetingList =
          greetings.stream().map(u -> u.greetingText).collect(Collectors.toList());
      return "Hello (" + greetingList.size() + ") robots: " + greetingList + "!";
    }
  }

  // Main workflow method
  public static class AccumulatorWorkflowImpl implements AccumulatorWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    private static final Logger logger = LoggerFactory.getLogger(AccumulatorWorkflowImpl.class);
    String bucketKey;
    ArrayDeque<Greeting> greetings;
    HashSet<String> allGreetingsSet;
    boolean exitRequested = false;
    ArrayDeque<Greeting> unprocessedGreetings = new ArrayDeque<Greeting>();

    @Override
    public String accumulateGreetings(
        String bucketKeyInput, Deque<Greeting> greetingsInput, Set<String> allGreetingsSetInput) {
      bucketKey = bucketKeyInput;
      greetings = new ArrayDeque<Greeting>();
      allGreetingsSet = new HashSet<String>();
      greetings.addAll(greetingsInput);
      allGreetingsSet.addAll(allGreetingsSetInput);

      // If you want to wait for a fixed amount of time instead of a time after a
      // message
      // as this does now, you might want to check out
      // ../../updatabletimer

      // Main Workflow Loop:
      // - wait for signals to come in
      // - every time a signal comes in, wait again for MAX_AWAIT_TIME
      // - if time runs out, and there are no messages, process them all and exit
      // - if exit signal is received, process any remaining signals and exit
      do {

        boolean timedout =
            !Workflow.await(MAX_AWAIT_TIME, () -> !unprocessedGreetings.isEmpty() || exitRequested);

        while (!unprocessedGreetings.isEmpty()) {
          processGreeting(unprocessedGreetings.removeFirst());
        }

        if (exitRequested || timedout) {
          String greetEveryone = processGreetings(greetings);

          if (unprocessedGreetings.isEmpty()) {
            logger.info("Greeting queue is still empty");
            return greetEveryone;
          } else {
            // you can get here if you send a signal after an exit, causing rollback just
            // after the
            // last processed activity
            logger.info("Greeting queue not empty, looping");
          }
        }
      } while (!unprocessedGreetings.isEmpty() || !Workflow.getInfo().isContinueAsNewSuggested());

      logger.info("starting continue as new processing");

      // Create a workflow stub that will be used to continue this workflow as a new
      AccumulatorWorkflow continueAsNew = Workflow.newContinueAsNewStub(AccumulatorWorkflow.class);

      // Request that the new run will be invoked by the Temporal system:
      continueAsNew.accumulateGreetings(bucketKey, greetings, allGreetingsSet);
      // this could be improved in the future with the are_handlers_finished API. For
      // now if a signal comes in
      // after this, it will fail the workflow task and retry handling the new
      // signal(s)

      return "continued as new; results passed to next run";
    }

    // Here is where we can process individual signals as they come in.
    // It's ok to call activities here.
    // This also validates an individual greeting:
    // - check for duplicates
    // - check for correct bucket
    public void processGreeting(Greeting greeting) {
      logger.info("processing greeting-" + greeting);
      if (greeting == null) {
        logger.warn("Greeting is null:" + greeting);
        return;
      }

      // this just ignores incorrect buckets - you can use workflowupdate to validate
      // and reject
      // bad bucket requests if needed
      if (!greeting.bucket.equals(bucketKey)) {
        logger.warn("wrong bucket, something is wrong with your signal processing: " + greeting);
        return;
      }

      if (!allGreetingsSet.add(greeting.greetingKey)) {
        logger.info("Duplicate signal event: " + greeting.greetingKey);
        return;
      }

      // add in any desired event processing activity here
      greetings.add(greeting);
    }

    private String processGreetings(Deque<Greeting> greetings) {
      logger.info("Composing greetings for: " + greetings);
      return activities.composeGreeting(greetings);
    }

    // Signal method
    // Keep it simple, these should be fast and not call activities
    @Override
    public void sendGreeting(Greeting greeting) {
      // signals can be the first workflow code that runs, make sure we have
      // an ArrayDeque to write to
      if (unprocessedGreetings == null) {
        unprocessedGreetings = new ArrayDeque<Greeting>();
      }
      logger.info("received greeting-" + greeting);
      unprocessedGreetings.add(greeting);
    }

    @Override
    public void exit() {
      logger.info("exit signal received");
      exitRequested = true;
    }
  }

  /**
   * With the Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws Exception {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query
     * Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);
    client.getWorkflowServiceStubs().healthCheck();

    /*
     * Define the workflow factory. It is used to create workflow workers for a
     * specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue
     * and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(AccumulatorWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless
     * and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    System.out.println("Worker started for task queue: " + TASK_QUEUE);

    // setup which tests to run
    // by default it will run an accumulation with a few (20) signals
    // to a set of 4 buckets with Signal To Start
    boolean testContinueAsNew = false;

    boolean testSignalEdgeCases = true;
    // configure signal edge cases to test
    boolean testSignalAfterWorkflowExit = true;
    boolean testSignalAfterExitSignal = !testSignalAfterWorkflowExit;
    boolean testDuplicate = true;
    boolean testIgnoreBadBucket = true;

    // setup to send signals
    String bucket = "blue";
    String workflowId = WORKFLOW_ID_PREFIX + "-" + bucket;
    ArrayDeque<Greeting> greetingList = new ArrayDeque<Greeting>();
    HashSet<String> allGreetingsSet = new HashSet<String>();
    String greetingKey = "key-";
    String greetingText = "Robby Robot";
    Greeting starterGreeting = new Greeting(greetingText, bucket, greetingKey);
    final String[] buckets = {"red", "blue", "green", "yellow"};
    final String[] names = {"Genghis Khan", "Missy", "Bill", "Ted", "Rufus", "Abe"};

    // Create the workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build();
    AccumulatorWorkflow workflow =
        client.newWorkflowStub(AccumulatorWorkflow.class, workflowOptions);

    // send many signals to start several workflows
    int max_signals = 20;

    if (testContinueAsNew) max_signals = 10000;
    for (int i = 0; i < max_signals; i++) {
      Random randomBucket = new Random();
      int bucketIndex = randomBucket.nextInt(buckets.length);
      bucket = buckets[bucketIndex];
      starterGreeting.setBucket(bucket);
      Thread.sleep(20); // simulate some delay

      workflowId = WORKFLOW_ID_PREFIX + "-" + bucket;

      Random randomName = new Random();
      int nameIndex = randomName.nextInt(names.length);
      starterGreeting.setGreetingText(names[nameIndex] + " Robot");

      workflowOptions =
          WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build();

      // Create the workflow client stub. It is used to start the workflow execution.
      workflow = client.newWorkflowStub(AccumulatorWorkflow.class, workflowOptions);

      BatchRequest request = client.newSignalWithStartRequest();
      starterGreeting.greetingKey = greetingKey + i;
      request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
      request.add(workflow::sendGreeting, starterGreeting);
      client.signalWithStart(request);
    }

    // Demonstrate we still can connect to WF and get result using untyped:
    if (max_signals > 0) {
      WorkflowStub untyped = WorkflowStub.fromTyped(workflow);

      // wait for it to finish
      try {
        String greeting = untyped.getResult(String.class);
        printWorkflowStatus(client, workflowId);
        System.out.println("Greeting: " + greeting);
      } catch (WorkflowFailedException e) {
        System.out.println("Workflow failed: " + e.getCause().getMessage());
        printWorkflowStatus(client, workflowId);
      }
    }
    if (!testSignalEdgeCases) {
      System.exit(0); // skip other demonstrations below
    }

    // set workflow parameters
    bucket = "purple";
    greetingList = new ArrayDeque<Greeting>();
    allGreetingsSet = new HashSet<String>();
    workflowId = WORKFLOW_ID_PREFIX + "-" + bucket;
    workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build();

    starterGreeting = new Greeting("Suzy Robot", bucket, "11235813");

    // Create the workflow client stub. It is used to start the workflow execution.
    AccumulatorWorkflow workflowSync =
        client.newWorkflowStub(AccumulatorWorkflow.class, workflowOptions);

    // Start workflow asynchronously and call its getGreeting workflow method
    WorkflowClient.start(workflowSync::accumulateGreetings, bucket, greetingList, allGreetingsSet);

    // After start for accumulateGreetings returns, the workflow is guaranteed to be
    // started, so we can send a signal to it using the workflow stub.
    // This workflow keeps receiving signals until exit is called or the timer
    // finishes with no
    // signals

    // When the workflow is started the accumulateGreetings will block for the
    // previously defined conditions
    // Send the first workflow signal
    workflowSync.sendGreeting(starterGreeting);

    // Test sending an exit, waiting for workflow exit, then sending a signal.
    // This will trigger a WorkflowNotFoundException if using the same workflow
    // handle
    if (testSignalAfterWorkflowExit) {
      workflowSync.exit();
      String greetingsAfterExit =
          workflowSync.accumulateGreetings(bucket, greetingList, allGreetingsSet);
      System.out.println(greetingsAfterExit);
    }

    // Test sending an exit, not waiting for workflow to exit, then sending a signal
    // this demonstrates Temporal history rollback
    // see https://community.temporal.io/t/continueasnew-signals/1008/7
    if (testSignalAfterExitSignal) {
      workflowSync.exit();
    }

    // Test sending more signals after workflow exit
    try {
      // send a second workflow signal
      Greeting janeGreeting = new Greeting("Jane Robot", bucket, "112358132134");
      workflowSync.sendGreeting(janeGreeting);

      if (testIgnoreBadBucket) {
        // send a third signal with an incorrect bucket - this will be ignored
        // can use workflow update to validate and reject a request if needed
        workflowSync.sendGreeting(new Greeting("Sally Robot", "taupe", "112358132134"));
      }

      if (testDuplicate) {
        // intentionally send a duplicate signal
        workflowSync.sendGreeting(janeGreeting);
      }

      if (!testSignalAfterWorkflowExit) {
        // wait for results if we haven't waited for them yet
        String greetingsAfterExit =
            workflowSync.accumulateGreetings(bucket, greetingList, allGreetingsSet);
        System.out.println(greetingsAfterExit);
      }
    } catch (WorkflowNotFoundException e) {
      System.out.println("Workflow not found - this is intentional: " + e.getCause().getMessage());
      printWorkflowStatus(client, workflowId);
    }

    try {
      /*
       * Here we create a new workflow stub using the same workflow id.
       * We do this to demonstrate that to send a signal to an already running
       * workflow you only need to know its workflow id.
       */
      AccumulatorWorkflow workflowById =
          client.newWorkflowStub(AccumulatorWorkflow.class, workflowId);

      Greeting laterGreeting = new Greeting("XVX Robot", bucket, "1123581321");
      // Send the second signal to our workflow
      workflowById.sendGreeting(laterGreeting);

      // Now let's send our exit signal to the workflow
      workflowById.exit();

      /*
       * We now call our accumulateGreetings workflow method synchronously after our
       * workflow has started.
       * This reconnects our workflowById workflow stub to the existing workflow and
       * blocks until a result is available. Note that this behavior assumes that
       * WorkflowOptions
       * are not configured with WorkflowIdReusePolicy.AllowDuplicate. If they were,
       * this call would fail
       * with the WorkflowExecutionAlreadyStartedException exception.
       * You can use the policy to force workflows for a new time period, e.g. a
       * collection day, to have a new workflow ID.
       */

      String greetings = workflowById.accumulateGreetings(bucket, greetingList, allGreetingsSet);

      // Print our results for greetings which were sent by signals
      System.out.println(greetings);
    } catch (WorkflowNotFoundException e) {
      System.out.println("Workflow not found - this is intentional: " + e.getCause().getMessage());
      printWorkflowStatus(client, workflowId);
    }

    /*
     * Here we try to send the signals as start to demonstrate that after a workflow
     * exited
     * and signals failed to send
     * we can send signals to a new workflow
     */
    if (testSignalAfterWorkflowExit) {
      greetingList = new ArrayDeque<Greeting>();
      allGreetingsSet = new HashSet<String>();
      workflowId = WORKFLOW_ID_PREFIX + "-" + bucket;

      Greeting laterGreeting = new Greeting("Final Robot", bucket, "1123");
      // Send the second signal to our workflow

      workflowOptions =
          WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build();

      // Create the workflow client stub. It is used to start the workflow execution.
      workflow = client.newWorkflowStub(AccumulatorWorkflow.class, workflowOptions);

      BatchRequest request = client.newSignalWithStartRequest();
      request.add(workflow::accumulateGreetings, bucket, greetingList, allGreetingsSet);
      request.add(workflow::sendGreeting, laterGreeting);
      client.signalWithStart(request);

      printWorkflowStatus(client, workflowId);

      String greetingsAfterExit =
          workflow.accumulateGreetings(bucket, greetingList, allGreetingsSet);

      // Print our results for greetings which were sent by signals
      System.out.println(greetingsAfterExit);

      printWorkflowStatus(client, workflowId);

      while (getWorkflowStatus(client, workflowId).equals("WORKFLOW_EXECUTION_STATUS_RUNNING")) {

        System.out.println("Workflow still running ");
        Thread.sleep(1000);
      }
    }

    System.exit(0);
  }

  private static void printWorkflowStatus(WorkflowClient client, String workflowId) {
    WorkflowStub existingUntyped =
        client.newUntypedWorkflowStub(workflowId, Optional.empty(), Optional.empty());
    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setExecution(existingUntyped.getExecution())
            .build();

    DescribeWorkflowExecutionResponse resp =
        client
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(describeWorkflowExecutionRequest);
    System.out.println(
        "**** PARENT: " + resp.getWorkflowExecutionInfo().getParentExecution().getWorkflowId());

    WorkflowExecutionInfo workflowExecutionInfo = resp.getWorkflowExecutionInfo();
    System.out.println("Workflow Status: " + workflowExecutionInfo.getStatus().toString());
  }

  private static String getWorkflowStatus(WorkflowClient client, String workflowId) {
    WorkflowStub existingUntyped =
        client.newUntypedWorkflowStub(workflowId, Optional.empty(), Optional.empty());
    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setExecution(existingUntyped.getExecution())
            .build();

    DescribeWorkflowExecutionResponse resp =
        client
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(describeWorkflowExecutionRequest);
    System.out.println(
        "**** PARENT: " + resp.getWorkflowExecutionInfo().getParentExecution().getWorkflowId());

    WorkflowExecutionInfo workflowExecutionInfo = resp.getWorkflowExecutionInfo();
    return workflowExecutionInfo.getStatus().toString();
  }
}
