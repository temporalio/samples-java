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
package com.uber.cadence.samples.fileprocessing;

import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.workflow.Workflow;
import java.net.URL;
import java.time.Duration;

/**
 * This implementation of FileProcessingWorkflow downloads the file, zips it and
 * uploads it to a destination. An important requirement for such workflow is that
 * while a first activity can run on any host, the second and third must run on the same host
 * as the first one. It is achieved through use of a host specific task list. The first activity
 * returns the name of the host specific task list and all others are dispatched using stub that
 * is configured with it. It assumes that FileProcessingWorker has a Worker running on the same
 * task list.
 */
public class FileProcessingWorkflowImpl implements FileProcessingWorkflow {

    // Uses default task list shared by the pool of workers.
    private final StoreActivities defaultTaskListStore;

    public FileProcessingWorkflowImpl() {
        // Create activity clients
        ActivityOptions ao = new ActivityOptions.Builder()
                .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                .setTaskList(FileProcessingWorker.TASK_LIST)
                .build();
        this.defaultTaskListStore = Workflow.newActivityStub(StoreActivities.class, ao);
    }

    @Override
    public void processFile(URL source, URL destination) {
        StoreActivities.TaskListFileNamePair downloaded = defaultTaskListStore.download(source);

        // Now initialize stubs that are specific to the returned task list.
        ActivityOptions hostActivityOptions = new ActivityOptions.Builder()
                .setTaskList(downloaded.getHostTaskList())
                .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                .build();
        StoreActivities hostSpecificStore = Workflow.newActivityStub(StoreActivities.class, hostActivityOptions);

        // Call processFile activity to zip the file
        // Call the activity to process the file using worker specific task list
        String processed = hostSpecificStore.process(downloaded.getFileName());
        // Call upload activity to upload zipped file
        hostSpecificStore.upload(processed, destination);
    }
}

