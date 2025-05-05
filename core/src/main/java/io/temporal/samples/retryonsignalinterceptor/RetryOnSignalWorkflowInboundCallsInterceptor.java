

package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;

public class RetryOnSignalWorkflowInboundCallsInterceptor
    extends WorkflowInboundCallsInterceptorBase {

  public RetryOnSignalWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
    super.init(new RetryOnSignalWorkflowOutboundCallsInterceptor(outboundCalls));
  }
}
