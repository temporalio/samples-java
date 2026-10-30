package io.temporal.samples.googleadk.chat;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.googleadk.session.Sessions;
import io.temporal.samples.googleadk.AgentSupport;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflow.WorkflowLock;

// @@@SNIPSTART java-google-adk-chat-workflow
public class ChatWorkflowImpl implements ChatWorkflow {
  private final WorkflowLock turnLock = Workflow.newWorkflowLock();
  private final Runner runner;
  private boolean finished;
  private String reply;
  private int turns;

  @WorkflowInit
  public ChatWorkflowImpl(ChatInput input) {
    reply = input.lastReply();
    LlmAgent agent =
        LlmAgent.builder()
            .name("chat_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Answer concisely, remembering the earlier turns in this conversation.")
            .build();
    runner = TemporalAdk.runnerBuilder().appName("chat").agent(agent).build();
    if (input.session() != null) {
      Sessions.importSession(runner.sessionService(), input.session());
    }
  }

  @Override
  public String chat(ChatInput input) {
    Workflow.await(
        () ->
            finished || turns >= 3 || (turns > 0 && Workflow.getInfo().isContinueAsNewSuggested()));
    Workflow.await(Workflow::isEveryHandlerFinished);
    if (!finished) {
      Session snapshot =
          Sessions.exportSession(
              runner.sessionService(), "chat", AgentSupport.USER, AgentSupport.SESSION);
      Workflow.continueAsNew(new ChatInput(snapshot, reply));
    }
    return reply;
  }

  @Override
  public String message(String text) {
    turnLock.lock();
    try {
      reply =
          AgentSupport.lastText(
              TemporalAdk.run(
                  runner,
                  AgentSupport.USER,
                  AgentSupport.SESSION,
                  Content.fromParts(Part.fromText(text))));
      turns++;
      return reply;
    } finally {
      turnLock.unlock();
    }
  }

  @Override
  public void validateMessage(String text) {
    if (finished) {
      throw new IllegalStateException("The conversation is finishing.");
    }
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("The message must not be blank.");
    }
  }

  @Override
  public void finish() {
    finished = true;
  }

  @Override
  public String lastReply() {
    return reply;
  }
}
// @@@SNIPEND
