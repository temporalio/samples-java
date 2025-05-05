package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.common.interceptors.*;

/** Should be registered through WorkerFactoryOptions. */
public class RetryOnSignalWorkerInterceptor extends WorkerInterceptorBase {
  @Override
  public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
    return new RetryOnSignalWorkflowInboundCallsInterceptor(next);
  }

  @Override
  public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
    return next;
  }
}
