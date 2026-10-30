package io.temporal.samples.googleadk.mcp;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface McpWorkflow {
  @WorkflowMethod
  String echo(String message);
}
