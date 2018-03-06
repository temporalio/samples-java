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

import com.amazonaws.services.s3.AmazonS3;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.samples.common.ConfigHelper;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

/**
 * This is the process which hosts all workflows and activities in this sample
 */
public class FileProcessingWorker {

    static final String TASK_LIST = "FileProcessing";

    public static void main(String[] args) throws Exception {
        ConfigHelper configHelper = ConfigHelper.createConfig();
        WorkflowService.Iface swfService = configHelper.createWorkflowClient();
        AmazonS3 s3Client = configHelper.createS3Client();
        String domain = configHelper.getDomain();

        String localFolder = configHelper.getValueFromConfig(FileProcessingConfigKeys.ACTIVITY_WORKER_LOCALFOLDER);

        // Start worker to poll the common task list
        final Worker workerForCommonTaskList = new Worker(swfService, domain, TASK_LIST, null);
        SimpleStoreActivitiesS3Impl storeActivityImpl = new SimpleStoreActivitiesS3Impl(s3Client, localFolder, getHostName());
        workerForCommonTaskList.registerActivitiesImplementations(storeActivityImpl);
        workerForCommonTaskList.registerWorkflowImplementationTypes(FileProcessingWorkflowZipImpl.class);

        workerForCommonTaskList.start();
        System.out.println("Worker tarted for task list: " + TASK_LIST);

        // Start worker to poll the host specific task list
        WorkerOptions hostSpecificOptions = new WorkerOptions.Builder().build();
        final Worker workerForHostSpecificTaskList = new Worker(swfService, domain, getHostName(), hostSpecificOptions);
        FileProcessingActivitiesZipImpl processorActivityImpl = new FileProcessingActivitiesZipImpl(localFolder);
        workerForHostSpecificTaskList.registerActivitiesImplementations(storeActivityImpl, processorActivityImpl);
        workerForHostSpecificTaskList.start();
        System.out.println("Worker Started for activity and workflow task List: " + getHostName());

        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                workerForCommonTaskList.shutdown(Duration.ofSeconds(5));
                workerForHostSpecificTaskList.shutdown(Duration.ofSeconds(5));
                System.out.println("Activity Workers Exited.");
            }
        });

        System.out.println("Please press any key to terminate service.");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);

    }

    static String getHostName() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

}
