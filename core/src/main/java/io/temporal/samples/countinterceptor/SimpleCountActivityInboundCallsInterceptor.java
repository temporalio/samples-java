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
