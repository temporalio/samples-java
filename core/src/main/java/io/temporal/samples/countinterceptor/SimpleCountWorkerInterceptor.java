

package io.temporal.samples.countinterceptor;

import io.temporal.common.interceptors.*;

public class SimpleCountWorkerInterceptor extends WorkerInterceptorBase {

  @Override
  public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
    return new SimpleCountWorkflowInboundCallsInterceptor(next);
  }

  @Override
  public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
    return new SimpleCountActivityInboundCallsInterceptor(next);
  }
}
