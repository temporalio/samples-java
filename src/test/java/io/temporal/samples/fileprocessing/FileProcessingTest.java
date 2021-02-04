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

package io.temporal.samples.fileprocessing;

import static io.temporal.samples.fileprocessing.FileProcessingWorker.TASK_QUEUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.temporal.api.enums.v1.TimeoutType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.TimeoutFailure;
import io.temporal.samples.fileprocessing.StoreActivities.TaskQueueFileNamePair;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

public class FileProcessingTest {

  private static final String HOST_NAME_1 = "host1";
  private static final String HOST_NAME_2 = "host2";

  private static final String FILE_NAME_UNPROCESSED = "input_file";
  private static final String FILE_NAME_PROCESSED = "output_file";

  private static final URL SOURCE;
  private static final URL DESTINATION;

  static {
    try {
      SOURCE = new URL("http://www.google.com/");
      DESTINATION = new URL("http://dummy");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Rule public Timeout globalTimeout = Timeout.seconds(2);

  /** Prints a history of the workflow under test in case of a test failure. */
  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  // Host specific workers.
  private Worker workerHost1;
  private Worker workerHost2;

  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(FileProcessingWorkflowImpl.class);
    workerHost1 = testEnv.newWorker(HOST_NAME_1);
    workerHost2 = testEnv.newWorker(HOST_NAME_2);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testHappyPath() {
    StoreActivities activities = mock(StoreActivities.class);
    when(activities.download(any()))
        .thenReturn(new TaskQueueFileNamePair(HOST_NAME_1, FILE_NAME_UNPROCESSED));
    worker.registerActivitiesImplementations(activities);

    StoreActivities activitiesHost1 = mock(StoreActivities.class);
    when(activitiesHost1.process(FILE_NAME_UNPROCESSED)).thenReturn(FILE_NAME_PROCESSED);
    workerHost1.registerActivitiesImplementations(activitiesHost1);

    StoreActivities activitiesHost2 = mock(StoreActivities.class);
    workerHost2.registerActivitiesImplementations(activitiesHost2);

    testEnv.start();
    FileProcessingWorkflow workflow =
        client.newWorkflowStub(
            FileProcessingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    // Execute workflow waiting for completion.
    workflow.processFile(SOURCE, DESTINATION);

    verify(activities).download(SOURCE);
    verify(activitiesHost1).process(FILE_NAME_UNPROCESSED);
    verify(activitiesHost1).upload(FILE_NAME_PROCESSED, DESTINATION);

    verifyNoMoreInteractions(activities, activitiesHost1);

    verifyNoInteractions(activitiesHost2);
  }

  @Test
  public void testHostFailover() {
    StoreActivities activities = mock(StoreActivities.class);
    when(activities.download(any()))
        .thenReturn(new TaskQueueFileNamePair(HOST_NAME_1, FILE_NAME_UNPROCESSED))
        .thenReturn(new TaskQueueFileNamePair(HOST_NAME_2, FILE_NAME_UNPROCESSED));

    worker.registerActivitiesImplementations(activities);

    StoreActivities activitiesHost1 = mock(StoreActivities.class);
    when(activitiesHost1.process(FILE_NAME_UNPROCESSED))
        .thenThrow(
            new TimeoutFailure("simulated", null, TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_START));
    workerHost1.registerActivitiesImplementations(activitiesHost1);

    StoreActivities activitiesHost2 = mock(StoreActivities.class);
    when(activitiesHost2.process(FILE_NAME_UNPROCESSED)).thenReturn(FILE_NAME_PROCESSED);

    workerHost2.registerActivitiesImplementations(activitiesHost2);

    testEnv.start();

    FileProcessingWorkflow workflow =
        client.newWorkflowStub(
            FileProcessingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    workflow.processFile(SOURCE, DESTINATION);

    verify(activities, times(2)).download(SOURCE);

    // TODO(maxim): Change to 1, once retry of SimulatedTimeoutException is not happening.
    // https://github.com/temporalio/temporal-java-sdk/issues/94
    verify(activitiesHost1, times(4)).process(FILE_NAME_UNPROCESSED);

    verify(activitiesHost2).process(FILE_NAME_UNPROCESSED);
    verify(activitiesHost2).upload(FILE_NAME_PROCESSED, DESTINATION);

    verifyNoMoreInteractions(activities, activitiesHost1, activitiesHost2);
  }
}
