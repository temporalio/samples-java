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

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

/**
 * This implementation of FileProcessingWorkflow downloads the file, zips it, and uploads it to a
 * destination. An important requirement for such a workflow is that while a first activity can run
 * on any host, the second and third must run on the same host as the first one. This is achieved
 * through use of a host specific task queue. The first activity returns the name of the host
 * specific task queue and all other activities are dispatched using the stub that is configured
 * with it. This assumes that FileProcessingWorker has a worker running on the same task queue.
 */
public class FileProcessingWorkflowImpl implements FileProcessingWorkflow {

  // Uses the default task queue shared by the pool of workers.
  private final StoreActivities defaultTaskQueueActivities;

  public FileProcessingWorkflowImpl() {
    // Create activity clients.
    ActivityOptions ao =
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(20))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumAttempts(4)
                    .setDoNotRetry(IllegalArgumentException.class.getName())
                    .build())
            .build();
    this.defaultTaskQueueActivities = Workflow.newActivityStub(StoreActivities.class, ao);
  }

  @Override
  public void processFile(URL source, URL destination) {
    RetryOptions retryOptions =
        RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(1)).build();
    // Retries the whole sequence on any failure, potentially on a different host.
    Workflow.retry(retryOptions, Optional.empty(), () -> processFileImpl(source, destination));
  }

  private void processFileImpl(URL source, URL destination) {
    StoreActivities.TaskQueueFileNamePair downloaded = defaultTaskQueueActivities.download(source);

    // Now initialize stubs that are specific to the returned task queue.
    ActivityOptions hostActivityOptions =
        ActivityOptions.newBuilder()
            .setTaskQueue(downloaded.getHostTaskQueue())
            // Set the amount a time an activity task can stay in the task queue before its picked
            // up by a Worker. It allows us to support cases where
            // the activity worker crashes or restarts before the activity starts execution.
            // This timeout should be specified only when host specific activity task queues are
            // used like in this sample.
            // Note that scheduleToStart timeout is not retryable and retry options will ignore it.
            // This timeout has to be handled by Workflow code.
            .setScheduleToStartTimeout(Duration.ofSeconds(10))
            // Set the max time of a single activity execution attempt.
            // Activity is going to be executed by a Worker listening to the specified
            // host task queue. If the activity is started but then the activity worker crashes
            // for some reason, we want to make sure that it is retried after the specified timeout.
            // This timeout should be be as short as the longest possible execution of the Activity.
            .setStartToCloseTimeout(Duration.ofSeconds(2))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumAttempts(4)
                    .setDoNotRetry(IllegalArgumentException.class.getName())
                    .build())
            .build();
    StoreActivities hostSpecificStore =
        Workflow.newActivityStub(StoreActivities.class, hostActivityOptions);

    // Call processFile activity to zip the file.
    // Call the activity to process the file using worker-specific task queue.
    String processed = hostSpecificStore.process(downloaded.getFileName());
    // Call upload activity to upload the zipped file.
    hostSpecificStore.upload(processed, destination);
  }
}
