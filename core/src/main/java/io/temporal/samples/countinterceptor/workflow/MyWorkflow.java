

package io.temporal.samples.countinterceptor.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {
  @WorkflowMethod
  String exec();

  @SignalMethod
  void signalNameAndTitle(String greeting, String title);

  @SignalMethod
  void exit();

  @QueryMethod
  String queryName();

  @QueryMethod
  String queryTitle();
}
