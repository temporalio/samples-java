package io.temporal.samples.googleadk.humanintheloop;

import io.temporal.googleadk.tool.Hitl;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface ApprovalWorkflow {
  @WorkflowMethod
  String fulfill(String question);

  @QueryMethod
  List<Hitl.PendingConfirmation> pendingConfirmations();

  @SignalMethod
  void decide(boolean confirmed);
}
