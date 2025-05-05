package io.temporal.samples.excludefrominterceptor.interceptor;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.samples.excludefrominterceptor.activities.ForInterceptorActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MyWorkflowOutboundCallsInterceptor extends WorkflowOutboundCallsInterceptorBase {
  private List<String> excludeWorkflowTypes = new ArrayList<>();
  private ForInterceptorActivities activities =
      Workflow.newActivityStub(
          ForInterceptorActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  public MyWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
    super(next);
  }

  public MyWorkflowOutboundCallsInterceptor(
      List<String> excludeWorkflowTypes, WorkflowOutboundCallsInterceptor next) {
    super(next);
    this.excludeWorkflowTypes = excludeWorkflowTypes;
  }

  @Override
  public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
    ActivityOutput output = super.executeActivity(input);
    if (!excludeWorkflowTypes.contains(Workflow.getInfo().getWorkflowType())) {
      // After activity completes we want to execute activity to lets say persist its result to db
      // or similar
      activities.forInterceptorActivityTwo(output.getResult().get());
    }
    return output;
  }
}
