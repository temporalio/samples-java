package io.temporal.samples.nexus_sync_operations.caller_remote;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface CallerRemoteWorkflow {
  @WorkflowMethod
  List<String> run();
}
