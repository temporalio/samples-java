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

package io.temporal.samples.excludefrominterceptor.interceptor;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.samples.excludefrominterceptor.activities.ForInterceptorActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MyWorkflowOutboundCallsInterceptor extends WorkflowOutboundCallsInterceptorBase {
  private List<String> excludeWorkflowTypes = new ArrayList<>();
  private ForInterceptorActivities activities =
      Workflow.newActivityStub(
          ForInterceptorActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  public MyWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
    super(next);
  }

  public MyWorkflowOutboundCallsInterceptor(
      List<String> excludeWorkflowTypes, WorkflowOutboundCallsInterceptor next) {
    super(next);
    this.excludeWorkflowTypes = excludeWorkflowTypes;
  }

  @Override
  public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
    ActivityOutput output = super.executeActivity(input);
    if (!excludeWorkflowTypes.contains(Workflow.getInfo().getWorkflowType())) {
      // After activity completes we want to execute activity to lets say persist its result to db
      // or similar
      activities.forInterceptorActivityTwo(output.getResult().get());
    }
    return output;
  }
}
