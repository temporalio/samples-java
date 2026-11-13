package io.temporal.samples.googleadk.multiagent;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface MultiAgentWorkflow {
  @WorkflowMethod
  List<String> plan(String question);
}
