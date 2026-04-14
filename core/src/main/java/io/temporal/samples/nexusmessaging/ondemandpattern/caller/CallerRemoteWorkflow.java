package io.temporal.samples.nexusmessaging.ondemandpattern.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface CallerRemoteWorkflow {
  @WorkflowMethod
  List<String> run();
}
