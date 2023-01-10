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

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;

public class SimpleCountWorkflowInboundCallsInterceptor
    extends WorkflowInboundCallsInterceptorBase {

  private WorkflowInfo workflowInfo;

  public SimpleCountWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
    this.workflowInfo = Workflow.getInfo();
    super.init(new SimpleCountWorkflowOutboundCallsInterceptor(outboundCalls));
  }

  @Override
  public WorkflowOutput execute(WorkflowInput input) {
    WorkerCounter.add(this.workflowInfo.getWorkflowId(), WorkerCounter.NUM_OF_WORKFLOW_EXECUTIONS);
    return super.execute(input);
  }

  @Override
  public void handleSignal(SignalInput input) {
    WorkerCounter.add(this.workflowInfo.getWorkflowId(), WorkerCounter.NUM_OF_SIGNALS);
    super.handleSignal(input);
  }

  @Override
  public QueryOutput handleQuery(QueryInput input) {
    WorkerCounter.add(this.workflowInfo.getWorkflowId(), WorkerCounter.NUM_OF_QUERIES);
    return super.handleQuery(input);
  }
}
