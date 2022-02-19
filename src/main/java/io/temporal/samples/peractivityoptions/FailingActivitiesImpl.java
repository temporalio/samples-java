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

import io.temporal.activity.Activity;

public class FailingActivitiesImpl implements FailingActivities {
  @Override
  public void activityTypeA() {
    // Get the activity type
    String type = Activity.getExecutionContext().getInfo().getActivityType();
    // Get the retry attempt
    int attempt = Activity.getExecutionContext().getInfo().getAttempt();
    // Wrap checked exception and throw
    throw Activity.wrap(
        new NullPointerException("Activity type: " + type + " attempt times: " + attempt));
  }

  @Override
  public void activityTypeB() {
    // Get the activity type
    String type = Activity.getExecutionContext().getInfo().getActivityType();
    // Get the retry attempt
    int attempt = Activity.getExecutionContext().getInfo().getAttempt();
    // Wrap checked exception and throw
    throw Activity.wrap(
        new NullPointerException("Activity type: " + type + " attempt times: " + attempt));
  }
}
