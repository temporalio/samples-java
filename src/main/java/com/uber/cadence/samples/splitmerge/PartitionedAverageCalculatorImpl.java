/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.uber.cadence.samples.splitmerge;

import com.uber.cadence.workflow.ActivityOptions;
import com.uber.cadence.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.uber.cadence.samples.splitmerge.SplitMergeWorker.TASK_LIST;

public class PartitionedAverageCalculatorImpl implements PartitionedAverageCalculator {

    private final AverageCalculatorActivities client;

    private final int numberOfWorkers;

    private final String bucketName;

    public PartitionedAverageCalculatorImpl(int numberOfWorkers, String bucketName) {
        this.numberOfWorkers = numberOfWorkers;
        this.bucketName = bucketName;
        ActivityOptions options = new ActivityOptions();
        options.setHeartbeatTimeoutSeconds(10);
        options.setStartToCloseTimeoutSeconds(30);
        options.setScheduleToStartTimeoutSeconds(30);
        options.setScheduleToCloseTimeoutSeconds(60);
        options.setTaskList(TASK_LIST);
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
        List<Future<Integer>> asyncResults = new ArrayList<>();
        for (int chunkNumber = 0; chunkNumber < numberOfWorkers; chunkNumber++) {
            // Splitting computation for each chunk as separate activity
            Future<Integer> result = Workflow.async(client::computeSumForChunk, bucketName, inputFile, chunkNumber, chunkSize);
            asyncResults.add(result);
        }
        // Merge phase
        try {
            int totalSum = 0;
            for (Future<Integer> workerSum : asyncResults) {
                totalSum += workerSum.get();
            }
            return (double) totalSum / (double) dataSize;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportResult(double result) {
        client.reportResult(result);
    }

}
