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
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.samples.excludefrominterceptor.activities.ForInterceptorActivities;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MyWorkflowInboundCallsInterceptor extends WorkflowInboundCallsInterceptorBase {
  private WorkflowInfo workflowInfo;
  private List<String> excludeWorkflowTypes = new ArrayList<>();
  private ForInterceptorActivities activities =
      Workflow.newActivityStub(
          ForInterceptorActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  public MyWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
    super(next);
  }

  public MyWorkflowInboundCallsInterceptor(
      List<String> excludeWorkflowTypes, WorkflowInboundCallsInterceptor next) {
    super(next);
    this.excludeWorkflowTypes = excludeWorkflowTypes;
  }

  @Override
  public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
    this.workflowInfo = Workflow.getInfo();
    super.init(new MyWorkflowOutboundCallsInterceptor(excludeWorkflowTypes, outboundCalls));
  }

  @Override
  public WorkflowOutput execute(WorkflowInput input) {
    WorkflowOutput output = super.execute(input);
    if (!excludeWorkflowTypes.contains(workflowInfo.getWorkflowType())) {
      // After workflow completes we want to execute activity to lets say persist its result to db
      // or similar
      activities.forInterceptorActivityOne(output.getResult());
    }
    return output;
  }
}
