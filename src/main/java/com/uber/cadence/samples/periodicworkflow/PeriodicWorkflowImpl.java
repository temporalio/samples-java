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
package com.uber.cadence.samples.periodicworkflow;

import com.uber.cadence.ActivityType;
import com.uber.cadence.workflow.ActivityOptions;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowThread;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.uber.cadence.samples.periodicworkflow.PeriodicWorkflowWorker.TASK_LIST;

public class PeriodicWorkflowImpl implements PeriodicWorkflow {

    private static final int SECOND = 1000;

    private final ErrorReportingActivities errorReporting;

    private final PeriodicWorkflow continueAsNew;

    ActivityOptions activityOptions;

    public PeriodicWorkflowImpl() {
        activityOptions = new ActivityOptions.Builder()
                .setHeartbeatTimeoutSeconds(10)
                .setStartToCloseTimeoutSeconds(30)
                .setScheduleToStartTimeoutSeconds(30)
                .setScheduleToCloseTimeoutSeconds(60)
                .setTaskList(TASK_LIST)
                .build();

        errorReporting = Workflow.newActivityStub(ErrorReportingActivities.class, activityOptions);

        continueAsNew = Workflow.newContinueAsNewStub(PeriodicWorkflow.class);
    }

    @Override
    public void startPeriodicWorkflow(final String activity, final Object[] activityArguments,
                                      final PeriodicWorkflowOptions options) {
        long startTime = Workflow.currentTimeMillis();

        // Use try catch to ensure that workflow is not going to fail as it causes new run not being created
        try {
            long continueAsNewAfter = TimeUnit.SECONDS.toMillis(options.getContinueAsNewAfterSeconds());
            while ((Workflow.currentTimeMillis() - startTime) < continueAsNewAfter) {

                // Call activity using dynamic client. Return type is specified as Void as it is not used, but activity that
                // returns some other type can be called this way.
                Promise<Object> activityCompletion = Workflow.executeActivityAsync(
                        activity, activityOptions, Object.class, activityArguments);

                if (options.isWaitForActivityCompletion()) {
                    activityCompletion.get();
                }
                // Sleep to re-run your periodic activity after activity completion,
                // but not earlier then after delay of executionPeriodSeconds.
                // However in a real cron workflow, the delay should be calculated every time to run an activity at
                // a predefined time.
                WorkflowThread.sleep(Duration.ofSeconds(options.getExecutionPeriodSeconds()));
            }
        } catch (Exception e) {
            errorReporting.reportFailure(e);
        } finally {
            long secondsLeft = options.getCompleteAfterSeconds() - (Workflow.currentTimeMillis() - startTime) / SECOND;
            if (secondsLeft > 0) {
                // This workflow run stops executing at the following line
                // and the new workflow run with the same workflow id is started with
                // passed arguments.
                options.setCompleteAfterSeconds(secondsLeft);
                continueAsNew.startPeriodicWorkflow(activity, activityArguments, options);
            }
        }
    }
}
