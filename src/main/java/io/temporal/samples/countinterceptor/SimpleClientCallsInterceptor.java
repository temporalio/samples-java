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

import io.temporal.common.interceptors.WorkflowClientCallsInterceptor;
import io.temporal.common.interceptors.WorkflowClientCallsInterceptorBase;
import java.util.concurrent.TimeoutException;

public class SimpleClientCallsInterceptor extends WorkflowClientCallsInterceptorBase {
  private ClientCounter clientCounter;

  public SimpleClientCallsInterceptor(
      WorkflowClientCallsInterceptor next, ClientCounter clientCounter) {
    super(next);
    this.clientCounter = clientCounter;
  }

  @Override
  public WorkflowStartOutput start(WorkflowStartInput input) {
    clientCounter.addStartInvocation(input.getWorkflowId());
    return super.start(input);
  }

  @Override
  public WorkflowSignalOutput signal(WorkflowSignalInput input) {
    clientCounter.addSignalInvocation(input.getWorkflowExecution().getWorkflowId());
    return super.signal(input);
  }

  @Override
  public <R> GetResultOutput<R> getResult(GetResultInput<R> input) throws TimeoutException {
    clientCounter.addGetResultInvocation(input.getWorkflowExecution().getWorkflowId());
    return super.getResult(input);
  }

  @Override
  public <R> QueryOutput<R> query(QueryInput<R> input) {
    clientCounter.addQueryInvocation(input.getWorkflowExecution().getWorkflowId());
    return super.query(input);
  }
}
