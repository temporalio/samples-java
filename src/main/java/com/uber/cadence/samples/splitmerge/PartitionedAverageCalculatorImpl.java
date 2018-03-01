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
package com.uber.cadence.samples.splitmerge;

import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.workflow.Async;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.uber.cadence.samples.splitmerge.SplitMergeWorker.TASK_LIST;

public class PartitionedAverageCalculatorImpl implements PartitionedAverageCalculator {

    private final AverageCalculatorActivities client;

    private final int numberOfWorkers;

    private final String bucketName;

    public PartitionedAverageCalculatorImpl(int numberOfWorkers, String bucketName) {
        this.numberOfWorkers = numberOfWorkers;
        this.bucketName = bucketName;
        ActivityOptions options = new ActivityOptions.Builder()
                .setHeartbeatTimeout(Duration.ofSeconds(10))
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setScheduleToStartTimeout(Duration.ofSeconds(30))
                .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                .setTaskList(TASK_LIST)
                .build();
        this.client = Workflow.newActivityStub(AverageCalculatorActivities.class, options);
    }

    @Override
    public double computeAverage(String inputFile) {
        int dataSize = client.computeDataSizeForInputData(bucketName, inputFile);
        return computeAverageDistributed(inputFile, dataSize);
    }

    private double computeAverageDistributed(String inputFile, int dataSize) {
        int chunkSize = dataSize / numberOfWorkers;

        // Create an array list to hold the result returned by each worker
        List<Promise<Integer>> asyncResults = new ArrayList<>();
        for (int chunkNumber = 0; chunkNumber < numberOfWorkers; chunkNumber++) {
            // Splitting computation for each chunk as separate activity
            Promise<Integer> result = Async.invoke(client::computeSumForChunk,
                    bucketName, inputFile, chunkNumber, chunkSize);
            asyncResults.add(result);
        }
        // Merge phase
        int totalSum = 0;
        for (Promise<Integer> workerSum : asyncResults) {
            totalSum += workerSum.get();
        }
        return (double) totalSum / (double) dataSize;
    }

    @Override
    public void reportResult(double result) {
        client.reportResult(result);
    }
}
