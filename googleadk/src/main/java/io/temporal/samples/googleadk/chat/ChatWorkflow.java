package io.temporal.samples.googleadk.chat;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ChatWorkflow {
  @WorkflowMethod
  String chat(ChatInput input);

  @SignalMethod
  void message(String text);

  @SignalMethod
  void finish();

  @QueryMethod
  String lastReply();
}
