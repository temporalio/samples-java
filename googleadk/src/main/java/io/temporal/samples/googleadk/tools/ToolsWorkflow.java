package io.temporal.samples.googleadk.tools;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ToolsWorkflow {
  @WorkflowMethod
  String ask(String question);
}
