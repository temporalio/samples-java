

package io.temporal.samples.countinterceptor;

import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.workflow.Workflow;

public class SimpleCountWorkflowOutboundCallsInterceptor
    extends WorkflowOutboundCallsInterceptorBase {

  public SimpleCountWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public <R> ChildWorkflowOutput<R> executeChildWorkflow(ChildWorkflowInput<R> input) {
    WorkerCounter.add(
        Workflow.getInfo().getWorkflowId(), WorkerCounter.NUM_OF_CHILD_WORKFLOW_EXECUTIONS);
    return super.executeChildWorkflow(input);
  }
}
