package io.temporal.samples.springai.multimodel;

import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Implementation of the multi-model workflow.
 *
 * <p>This demonstrates how to use multiple AI providers in a single workflow:
 *
 * <ul>
 *   <li><b>openai</b> - Uses OpenAI gpt-4o-mini for quick, cost-effective responses
 *   <li><b>anthropic</b> - Uses Anthropic Claude for complex reasoning tasks
 *   <li><b>default</b> - Uses the primary/default model (OpenAI)
 * </ul>
 *
 * <p>The workflow shows three ways to create ActivityChatModel:
 *
 * <ol>
 *   <li>{@code ActivityChatModel.forDefault()} - Uses the default model
 *   <li>{@code ActivityChatModel.forModel("beanName")} - Uses a specific model by bean name
 *   <li>{@code ActivityChatModel.forModel("name", timeout, retries)} - Custom options
 * </ol>
 */
public class MultiModelWorkflowImpl implements MultiModelWorkflow {

  private final Map<String, ChatClient> chatClients;
  private String lastResponse = "";
  private boolean ended = false;
  private int messageCount = 0;

  @WorkflowInit
  public MultiModelWorkflowImpl() {
    chatClients = new HashMap<>();

    // Create a chat client using the default model
    // This uses the @Primary bean or the first ChatModel bean
    ActivityChatModel defaultModel = ActivityChatModel.forDefault();
    chatClients.put(
        "default",
        TemporalChatClient.builder(defaultModel)
            .defaultSystem("You are a helpful assistant. You are the DEFAULT model.")
            .build());

    // Create a chat client using OpenAI (gpt-4o-mini)
    // This references the bean name defined in ChatModelConfig
    ActivityChatModel openAiModel = ActivityChatModel.forModel("openAiChatModel");
    chatClients.put(
        "openai",
        TemporalChatClient.builder(openAiModel)
            .defaultSystem("You are a helpful assistant powered by OpenAI. Keep answers concise.")
            .build());

    // Create a chat client using Anthropic Claude with custom timeout
    // Complex reasoning might take longer, so we give it more time
    ActivityChatModel anthropicModel =
        ActivityChatModel.forModel(
            "anthropicChatModel",
            Duration.ofMinutes(5), // Longer timeout for complex reasoning
            3); // 3 retry attempts
    chatClients.put(
        "anthropic",
        TemporalChatClient.builder(anthropicModel)
            .defaultSystem(
                "You are a helpful assistant powered by Anthropic. "
                    + "You excel at careful reasoning and nuanced responses.")
            .build());
  }

  @Override
  public String run() {
    // Wait until the chat is ended
    Workflow.await(() -> ended);
    return "Chat ended after " + messageCount + " messages.";
  }

  @Override
  public void chat(String modelName, String message) {
    messageCount++;

    ChatClient client = chatClients.get(modelName);
    if (client == null) {
      lastResponse = "Unknown model: " + modelName + ". Available: " + chatClients.keySet();
      return;
    }

    lastResponse = client.prompt().user(message).call().content();
  }

  @Override
  public String getLastResponse() {
    return lastResponse;
  }

  @Override
  public void end() {
    ended = true;
  }
}
