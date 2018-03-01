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

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * This implementation of FileProcessingWorkflow downloads the file, zips it and
 * uploads it back to S3
 */
public class FileProcessingWorkflowZipImpl implements FileProcessingWorkflow {

    private static final int MAX_RETRIES = 10;

    // Uses default task list shared by the pool of workers.
    private final SimpleStoreActivities defaultTaskListStore;
    private List<String> history = new ArrayList<>();

    public FileProcessingWorkflowZipImpl() {
        // Create activity clients
        ActivityOptions ao = new ActivityOptions.Builder()
                .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                .setTaskList(FileProcessingWorker.TASK_LIST)
                .build();
        this.defaultTaskListStore = Workflow.newActivityStub(SimpleStoreActivities.class, ao);
    }

    @Override
    public void processFile(Arguments args) {
        history.add("Started");
        // Use runId as a way to ensure that downloaded files do not get name collisions
        String workflowRunId = Workflow.getContext().getWorkflowExecution().getRunId();
        File localSource = new File(args.getSourceBucketName());
        final String localSourceFilename = workflowRunId + "_" + localSource.getName();
        File localTarget = new File(args.getTargetFilename());
        final String localTargetFilename = workflowRunId + "_" + localTarget.getName();
        SimpleStoreActivities workerTaskListStore = null;
        // Very simple retry strategy. On any error reexecute the whole sequence from the beginning.
        // In real life each activity would have additional retry logic and policy.
        int retry = 0;
        Exception failureCause = null;
        while (retry++ < MAX_RETRIES) {
            try {
                // Worker specific task list returned by the activity
                String workerTaskList = defaultTaskListStore.download(args.getSourceBucketName(),
                        args.getSourceFilename(), localSourceFilename);
                history.add("Downloaded to " + workerTaskList);

                // Now initialize stubs that are specific to the returned task list.
                ActivityOptions hostAO = new ActivityOptions.Builder()
                        .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                        .setScheduleToStartTimeout(Duration.ofSeconds(10)) // short queueing timeout
                        .setTaskList(workerTaskList)
                        .build();
                workerTaskListStore = Workflow.newActivityStub(SimpleStoreActivities.class, hostAO);
                FileProcessingActivities workerTaskListProcessor = Workflow.newActivityStub(FileProcessingActivities.class, hostAO);

                // Call processFile activity to zip the file
                // Call the activity to process the file using worker specific task list
                workerTaskListProcessor.processFile(localSourceFilename, localTargetFilename);
                // Call upload activity to upload zipped file
                history.add("Processed at " + workerTaskList);
                workerTaskListStore.upload(args.getTargetBucketName(), args.getTargetFilename(), localTargetFilename);
                history.add("Completed");
                break; // Bail out of the retry loop
            } catch (Exception e) {
                history.add("Failed " + retry + " time:" + e.getMessage());
                failureCause = e;
                continue;
            } finally {
                if (workerTaskListStore != null) { // File was downloaded
                    // Call deleteLocalFile activity using the host specific task list
                    try {
                        workerTaskListStore.deleteLocalFile(localSourceFilename);
                        workerTaskListStore.deleteLocalFile(localTargetFilename);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
        if (failureCause != null) {
            throw Workflow.throwWrapped(failureCause);
        }
    }
}

