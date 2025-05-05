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
