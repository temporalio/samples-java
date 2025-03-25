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

package io.temporal.samples.nexuscontextpropagation.propagation;

import io.nexusrpc.OperationException;
import io.nexusrpc.handler.OperationContext;
import io.temporal.common.interceptors.NexusOperationInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import java.util.Map;
import org.slf4j.MDC;

/**
 * Propagates MDC context from the caller workflow to the Nexus service through the operation
 * headers.
 */
public class NexusMDCContextInterceptor extends WorkerInterceptorBase {
  private static final String NEXUS_HEADER_PREFIX = "x-nexus-";

  @Override
  public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
    return new WorkflowInboundCallsInterceptorNexusMDC(next);
  }

  public static class WorkflowInboundCallsInterceptorNexusMDC
      extends io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase {
    private final WorkflowInboundCallsInterceptor next;

    public WorkflowInboundCallsInterceptorNexusMDC(WorkflowInboundCallsInterceptor next) {
      super(next);
      this.next = next;
    }

    @Override
    public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
      next.init(new WorkflowOutboundCallsInterceptorNexusMDC(outboundCalls));
    }
  }

  public static class WorkflowOutboundCallsInterceptorNexusMDC
      extends io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase {
    private final WorkflowOutboundCallsInterceptor next;

    public WorkflowOutboundCallsInterceptorNexusMDC(WorkflowOutboundCallsInterceptor next) {
      super(next);
      this.next = next;
    }

    @Override
    public <R> ExecuteNexusOperationOutput<R> executeNexusOperation(
        ExecuteNexusOperationInput<R> input) {
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      if (contextMap != null) {
        Map<String, String> headerMap = input.getHeaders();
        contextMap.forEach(
            (k, v) -> {
              if (k.startsWith(NEXUS_HEADER_PREFIX)) {
                headerMap.put(k, v);
              }
            });
      }
      return next.executeNexusOperation(input);
    }
  }

  @Override
  public NexusOperationInboundCallsInterceptor interceptNexusOperation(
      OperationContext context, NexusOperationInboundCallsInterceptor next) {
    return new NexusOperationInboundCallsInterceptorNexusMDC(next);
  }

  private static class NexusOperationInboundCallsInterceptorNexusMDC
      extends io.temporal.common.interceptors.NexusOperationInboundCallsInterceptorBase {
    private final NexusOperationInboundCallsInterceptor next;

    public NexusOperationInboundCallsInterceptorNexusMDC(
        NexusOperationInboundCallsInterceptor next) {
      super(next);
      this.next = next;
    }

    @Override
    public StartOperationOutput startOperation(StartOperationInput input)
        throws OperationException {
      input
          .getOperationContext()
          .getHeaders()
          .forEach(
              (k, v) -> {
                if (k.startsWith(NEXUS_HEADER_PREFIX)) {
                  MDC.put(k, v);
                }
              });
      return next.startOperation(input);
    }

    @Override
    public CancelOperationOutput cancelOperation(CancelOperationInput input) {
      input
          .getOperationContext()
          .getHeaders()
          .forEach(
              (k, v) -> {
                if (k.startsWith(NEXUS_HEADER_PREFIX)) {
                  MDC.put(k, v);
                }
              });
      return next.cancelOperation(input);
    }
  }
}
