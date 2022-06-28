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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.fileprocessing.StoreActivities.TaskQueueFileNamePair;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.Worker;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.*;

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

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(FileProcessingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  // Host specific workers.
  private Worker workerHost1;
  private Worker workerHost2;

  @Before
  public void setUp() {
    workerHost1 = testWorkflowRule.getTestEnvironment().newWorker(HOST_NAME_1);
    workerHost2 = testWorkflowRule.getTestEnvironment().newWorker(HOST_NAME_2);
  }

  @Test
  public void testHappyPath() {
    StoreActivities activities = mock(StoreActivities.class);
    when(activities.download(any()))
        .thenReturn(new TaskQueueFileNamePair(HOST_NAME_1, FILE_NAME_UNPROCESSED));
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);

    StoreActivities activitiesHost1 = mock(StoreActivities.class);
    when(activitiesHost1.process(FILE_NAME_UNPROCESSED)).thenReturn(FILE_NAME_PROCESSED);
    workerHost1.registerActivitiesImplementations(activitiesHost1);

    StoreActivities activitiesHost2 = mock(StoreActivities.class);
    workerHost2.registerActivitiesImplementations(activitiesHost2);

    testWorkflowRule.getTestEnvironment().start();

    FileProcessingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                FileProcessingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    // Execute workflow waiting for completion.
    workflow.processFile(SOURCE, DESTINATION);

    verify(activities).download(SOURCE);
    verify(activitiesHost1).process(FILE_NAME_UNPROCESSED);
    verify(activitiesHost1).upload(FILE_NAME_PROCESSED, DESTINATION);

    verifyNoMoreInteractions(activities, activitiesHost1);

    verifyNoInteractions(activitiesHost2);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test(timeout = 30_000)
  public void testHostFailover() {
    StoreActivities activities = mock(StoreActivities.class);
    when(activities.download(any()))
        .thenReturn(new TaskQueueFileNamePair(HOST_NAME_1, FILE_NAME_UNPROCESSED))
        .thenReturn(new TaskQueueFileNamePair(HOST_NAME_2, FILE_NAME_UNPROCESSED));

    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);

    StoreActivities activitiesHost1 = mock(StoreActivities.class);
    when(activitiesHost1.process(FILE_NAME_UNPROCESSED))
        .then(
            invocation -> {
              Thread.sleep(Long.MAX_VALUE);
              return "done";
            });

    workerHost1.registerActivitiesImplementations(activitiesHost1);

    StoreActivities activitiesHost2 = mock(StoreActivities.class);
    when(activitiesHost2.process(FILE_NAME_UNPROCESSED)).thenReturn(FILE_NAME_PROCESSED);

    workerHost2.registerActivitiesImplementations(activitiesHost2);

    testWorkflowRule.getTestEnvironment().start();

    FileProcessingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                FileProcessingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    workflow.processFile(SOURCE, DESTINATION);

    verify(activities, times(2)).download(SOURCE);

    // TODO(maxim): Change to 1, once retry of SimulatedTimeoutException is not happening.
    // https://github.com/temporalio/temporal-java-sdk/issues/94
    verify(activitiesHost1, times(4)).process(FILE_NAME_UNPROCESSED);

    verify(activitiesHost2).process(FILE_NAME_UNPROCESSED);
    verify(activitiesHost2).upload(FILE_NAME_PROCESSED, DESTINATION);

    verifyNoMoreInteractions(activities, activitiesHost1, activitiesHost2);

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
