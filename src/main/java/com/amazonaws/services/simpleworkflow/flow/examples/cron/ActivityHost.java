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
package com.amazonaws.services.simpleworkflow.flow.examples.cron;

import com.amazonaws.services.simpleworkflow.flow.examples.common.ConfigHelper;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerOptions;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This is the process which hosts all Activities in this sample
 */
public class ActivityHost {    
    
    static final String ACTIVITIES_TASK_LIST = "Periodic";

    public static void main(String[] args) throws Exception {
        ConfigHelper configHelper = ConfigHelper.createConfig();
        WorkflowService.Iface swfService = configHelper.createWorkflowClient();
        String domain = configHelper.getDomain();

        WorkerOptions options = new WorkerOptions();
        options.setDisableWorkflowWorker(true);
        final Worker worker = new Worker(swfService, domain, ACTIVITIES_TASK_LIST, options);

        // Create activity implementations
        CronExampleActivitiesImpl activities = new CronExampleActivitiesImpl();
        worker.addActivities(activities);

        worker.start();

        System.out.println("Activity Worker Started for Task List: " + ACTIVITIES_TASK_LIST);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            worker.shutdown(1, TimeUnit.MINUTES);
            System.out.println("Activity Worker Exited.");
        }));

        System.out.println("Please press any key to terminate service.");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
 
}


