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
import java.util.ArrayList;
import java.util.List;

public class ChatWorkflowImpl implements ChatWorkflow {
  private final List<String> messages = new ArrayList<>();
  private boolean finished;
  private String reply = "";

  @WorkflowInit
  public ChatWorkflowImpl(ChatInput input) {
    messages.addAll(input.messages());
    finished = input.finished();
    reply = input.lastReply();
  }

  // @@@SNIPSTART java-google-adk-chat-workflow
  @Override
  public String chat(ChatInput input) {
    LlmAgent agent =
        LlmAgent.builder()
            .name("chat_assistant")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction("Answer concisely, remembering the earlier turns in this conversation.")
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("chat").agent(agent).build();
    if (input.session() != null) {
      Sessions.importSession(runner.sessionService(), input.session());
    }
    int turns = 0;
    while (!finished || !messages.isEmpty()) {
      Workflow.await(() -> finished || !messages.isEmpty());
      if (messages.isEmpty()) {
        break;
      }
      String text = messages.remove(0);
      reply =
          AgentSupport.lastText(
              TemporalAdk.run(
                  runner,
                  AgentSupport.USER,
                  AgentSupport.SESSION,
                  Content.fromParts(Part.fromText(text))));
      // Use a small threshold to make Continue-As-New easy to observe in this sample.
      if (++turns == 3 && !finished) {
        Session snapshot =
            Sessions.exportSession(
                runner.sessionService(), "chat", AgentSupport.USER, AgentSupport.SESSION);
        Workflow.continueAsNew(new ChatInput(snapshot, List.copyOf(messages), finished, reply));
      }
    }
    return reply;
  }

  // @@@SNIPEND

  @Override
  public void message(String text) {
    messages.add(text);
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
