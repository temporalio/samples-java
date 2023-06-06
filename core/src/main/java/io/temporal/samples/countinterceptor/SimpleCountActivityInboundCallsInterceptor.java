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

package io.temporal.samples.countinterceptor;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;

public class SimpleCountActivityInboundCallsInterceptor
    extends ActivityInboundCallsInterceptorBase {

  private ActivityExecutionContext activityExecutionContext;

  public SimpleCountActivityInboundCallsInterceptor(ActivityInboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public void init(ActivityExecutionContext context) {
    this.activityExecutionContext = context;
    super.init(context);
  }

  @Override
  public ActivityOutput execute(ActivityInput input) {
    WorkerCounter.add(
        this.activityExecutionContext.getInfo().getWorkflowId(),
        WorkerCounter.NUM_OF_ACTIVITY_EXECUTIONS);
    return super.execute(input);
  }
}
