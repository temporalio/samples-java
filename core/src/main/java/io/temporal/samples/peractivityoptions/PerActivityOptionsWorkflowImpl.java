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

package io.temporal.samples.peractivityoptions;

import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

public class PerActivityOptionsWorkflowImpl implements PerActivityOptionsWorkflow {

  // Create activity stub that will inherit activity options set in WorkflowImplementationOptions
  // Note that you can overwrite the per-activity options by setting specific ActivityOptions
  // when creating activity stub here (Workflow.newActivityStub(FailingActivities.class, options)
  private FailingActivities activities = Workflow.newActivityStub(FailingActivities.class);

  private Logger logger = Workflow.getLogger(this.getClass().getName());

  @Override
  public void execute() {

    // Execute first activity
    try {
      activities.activityTypeA();
    } catch (ActivityFailure af) {
      // Activity invocations always throw ActivityFailure

      // Our activity was retried up to its set setStartToCloseTimeout
      // We exhausted our retries so cause of our failure is ApplicationFailure
      // From ApplicationFailure we can get our original NPE message
      logger.info("ActivityFailure cause: " + af.getCause().getClass().getName());
      logger.info("ApplicationFailure type: " + ((ApplicationFailure) af.getCause()).getType());
      // Original message should include a retry attempt number > 1
      logger.info(
          "Application Failure orig message: "
              + ((ApplicationFailure) af.getCause()).getOriginalMessage());
    }

    // Execute second activity
    try {
      activities.activityTypeB();
    } catch (ActivityFailure af) {
      logger.info("ActivityFailure cause: " + af.getCause().getClass().getName());
      logger.info("ApplicationFailure type: " + ((ApplicationFailure) af.getCause()).getType());
      // Original message should include a retry attempt number == 1 since we threw the doNotRetry
      // NPE
      // Set in the per activity type options
      logger.info(
          "Application Failure orig message: "
              + ((ApplicationFailure) af.getCause()).getOriginalMessage());
    }
  }
}
