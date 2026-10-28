package io.temporal.samples.googleadk.streaming;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface StreamingWorkflow {
  @WorkflowMethod
  List<ModelChunk> stream(String prompt);

  @SignalMethod
  void acknowledge();
}
