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
package com.uber.cadence.samples.booking;

import com.uber.cadence.WorkflowService;
import com.uber.cadence.samples.common.ConfigHelper;
import com.uber.cadence.worker.Worker;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This is the process which hosts all workflows and activities in this sample
 */
public class BookingWorker {
    private static WorkflowService.Iface swfService;
    private static Worker worker;
    private static String domain;


    public static void main(String[] args) throws Exception {
    	// load configuration
    	ConfigHelper configHelper = loadConfig();

    	// Start Activity Worker
        startWorker(configHelper);

        // Add a Shutdown hook to close ActivityWorker
        addShutDownHook();

        System.out.println("Please press any key to terminate service.");

        try {
            System.in.read();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);

    }

    private static void startWorker(ConfigHelper configHelper) throws Exception {
    	// Start worker to poll the common task list
    	String taskList = configHelper.getValueFromConfig(BookingConfigKeys.ACTIVITY_WORKER_TASKLIST);
        worker = new Worker(swfService, domain, taskList, null);
    	worker.addWorkflowImplementationType(BookingWorkflowImpl.class);
    	// Create activity implementations
    	BookingActivities bookingActivitiesImpl = new BookingActivitiesImpl();
    	worker.addActivitiesImplementation(bookingActivitiesImpl);
    	worker.start();
        System.out.println("Worker Started for Task List: " + taskList);
	}

    private static void stopWorker() throws InterruptedException {
        System.out.println("Stopping Worker...");
        worker.shutdown(10, TimeUnit.SECONDS);
        System.out.println("Worker Stopped...");
    }

    static ConfigHelper loadConfig() throws IllegalArgumentException, IOException{
       	ConfigHelper configHelper = ConfigHelper.createConfig();
        swfService = configHelper.createWorkflowClient();
        domain = configHelper.getDomain();
        return configHelper;
    }

    static void addShutDownHook(){
    	  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

              public void run() {
                  try {
                      stopWorker();
                  }
                  catch (InterruptedException e) {
                      e.printStackTrace();
                  }
              }
          }));
    }
}
