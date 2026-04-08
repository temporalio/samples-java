package io.temporal.samples.nexus_sync_operations.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface CallerWorkflow {
  @WorkflowMethod
  List<String> run();
}
