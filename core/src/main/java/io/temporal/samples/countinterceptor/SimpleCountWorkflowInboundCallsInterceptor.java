

package io.temporal.samples.countinterceptor;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;

public class SimpleCountWorkflowInboundCallsInterceptor
    extends WorkflowInboundCallsInterceptorBase {

  private WorkflowInfo workflowInfo;

  public SimpleCountWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
    this.workflowInfo = Workflow.getInfo();
    super.init(new SimpleCountWorkflowOutboundCallsInterceptor(outboundCalls));
  }

  @Override
  public WorkflowOutput execute(WorkflowInput input) {
    WorkerCounter.add(this.workflowInfo.getWorkflowId(), WorkerCounter.NUM_OF_WORKFLOW_EXECUTIONS);
    return super.execute(input);
  }

  @Override
  public void handleSignal(SignalInput input) {
    WorkerCounter.add(this.workflowInfo.getWorkflowId(), WorkerCounter.NUM_OF_SIGNALS);
    super.handleSignal(input);
  }

  @Override
  public QueryOutput handleQuery(QueryInput input) {
    WorkerCounter.add(this.workflowInfo.getWorkflowId(), WorkerCounter.NUM_OF_QUERIES);
    return super.handleQuery(input);
  }
}
