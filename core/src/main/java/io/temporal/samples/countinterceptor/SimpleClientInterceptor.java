package io.temporal.samples.countinterceptor;

import io.temporal.common.interceptors.WorkflowClientCallsInterceptor;
import io.temporal.common.interceptors.WorkflowClientInterceptorBase;

public class SimpleClientInterceptor extends WorkflowClientInterceptorBase {

  private ClientCounter clientCounter;

  public SimpleClientInterceptor(ClientCounter clientCounter) {
    this.clientCounter = clientCounter;
  }

  @Override
  public WorkflowClientCallsInterceptor workflowClientCallsInterceptor(
      WorkflowClientCallsInterceptor next) {
    return new SimpleClientCallsInterceptor(next, clientCounter);
  }
}
