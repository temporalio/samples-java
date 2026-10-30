package io.temporal.samples.googleadk.chat;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

// @@@SNIPSTART java-google-adk-chat-interface
@WorkflowInterface
public interface ChatWorkflow {
  @WorkflowMethod
  String chat(ChatInput input);

  @UpdateMethod
  String message(String text);

  @UpdateValidatorMethod(updateName = "message")
  void validateMessage(String text);

  @SignalMethod
  void finish();

  @QueryMethod
  String lastReply();
}
// @@@SNIPEND
