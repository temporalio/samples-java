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

import io.temporal.common.interceptors.*;
import java.util.ArrayList;
import java.util.List;

public class MyWorkerInterceptor extends WorkerInterceptorBase {
  private List<String> excludeWorkflowTypes = new ArrayList<>();
  private List<String> excludeActivityTypes = new ArrayList<>();

  public MyWorkerInterceptor() {}

  public MyWorkerInterceptor(List<String> excludeWorkflowTypes) {
    this.excludeWorkflowTypes = excludeWorkflowTypes;
  }

  public MyWorkerInterceptor(List<String> excludeWorkflowTypes, List<String> excludeActivityTypes) {
    this.excludeWorkflowTypes = excludeWorkflowTypes;
    this.excludeActivityTypes = excludeActivityTypes;
  }

  @Override
  public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
    return new MyWorkflowInboundCallsInterceptor(excludeWorkflowTypes, next);
  }

  @Override
  public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
    return new MyActivityInboundCallsInterceptor(excludeActivityTypes, next);
  }
}
