

package io.temporal.samples.excludefrominterceptor.interceptor;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import java.util.ArrayList;
import java.util.List;

public class MyActivityInboundCallsInterceptor extends ActivityInboundCallsInterceptorBase {

  private ActivityExecutionContext activityExecutionContext;
  private List<String> excludeActivityTypes = new ArrayList<>();

  public MyActivityInboundCallsInterceptor(ActivityInboundCallsInterceptor next) {
    super(next);
  }

  public MyActivityInboundCallsInterceptor(
      List<String> excludeActivityTypes, ActivityInboundCallsInterceptor next) {
    super(next);
    this.excludeActivityTypes = excludeActivityTypes;
  }

  @Override
  public void init(ActivityExecutionContext context) {
    this.activityExecutionContext = context;
    super.init(context);
  }

  @Override
  public ActivityOutput execute(ActivityInput input) {
    if (!excludeActivityTypes.contains(activityExecutionContext.getInfo().getActivityType())) {
      // If activity retry attempt is > X then we want to log this (or push to metrics or similar)
      // for demo we just use >=1 just to log and dont have to explicitly fail our sample activities
      if (activityExecutionContext.getInfo().getAttempt() >= 1) {
        System.out.println(
            "Activity retry attempt noted - "
                + activityExecutionContext.getInfo().getWorkflowType()
                + " - "
                + activityExecutionContext.getInfo().getActivityType());
      }
    }
    return super.execute(input);
  }
}
