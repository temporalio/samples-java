/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License is
 * located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.simpleworkflow.flow.examples.cron;

import com.uber.cadence.activity.Activity;
import com.uber.cadence.internal.ActivityTask;

public class CronExampleActivitiesImpl implements CronExampleActivities {

    @Override
    public void doSomeWork(String parameter) {
        ActivityTask task = Activity.getTask();
        String taskid = task.getActivityId();
        System.out.println("Processed activity task with id: " + taskid);
    }

}
