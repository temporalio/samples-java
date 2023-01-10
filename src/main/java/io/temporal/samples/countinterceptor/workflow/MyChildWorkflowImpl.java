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

package io.temporal.samples.countinterceptor.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.countinterceptor.activities.MyActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyChildWorkflowImpl implements MyChildWorkflow {
  @Override
  public String execChild(String name, String title) {
    MyActivities activities =
        Workflow.newActivityStub(
            MyActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    String result = activities.sayHello(name, title);
    result += activities.sayGoodBye(name, title);

    return result;
  }
}
