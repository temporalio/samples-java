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
