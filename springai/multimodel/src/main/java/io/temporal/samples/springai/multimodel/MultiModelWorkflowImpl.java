package io.temporal.samples.springai.multimodel;

import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Implementation of the multi-model workflow.
 *
 * <p>This demonstrates how to use multiple AI providers in a single workflow:
 *
 * <ul>
 *   <li><b>openai</b> - Uses OpenAI gpt-4o-mini for quick, cost-effective responses
 *   <li><b>anthropic</b> - Uses Anthropic Claude for complex reasoning tasks
 *   <li><b>think</b> - Uses Anthropic Claude with <i>extended thinking</i> enabled for hard
 *       problems. This demonstrates {@code temporal-spring-ai}'s provider-specific ChatOptions
 *       pass-through: an {@link AnthropicChatOptions} instance with a {@code thinking}
 *       configuration is attached per call, and every field survives the round-trip across the
 *       activity boundary.
 *   <li><b>default</b> - Uses the primary/default model (OpenAI)
 * </ul>
 *
 * <p>The workflow uses two patterns for wiring activity options:
 *
 * <ul>
 *   <li><b>Bean-based overrides (declarative, static)</b> — {@code ChatModelConfig} declares a
 *       {@code ChatModelActivityOptions} bean with per-model {@code ActivityOptions}. The workflow
 *       then just calls {@link ActivityChatModel#forDefault()} / {@link
 *       ActivityChatModel#forModel(String)}, which consult that bean automatically. That covers
 *       static per-model config (Anthropic needs a longer timeout because reasoning models are
 *       slow; OpenAI uses plugin defaults).
 *   <li><b>Per-call {@code ChatClient.defaultOptions(...)} (imperative, dynamic)</b> — the {@code
 *       think:} route builds an {@link AnthropicChatOptions} instance with {@code thinking=ENABLED}
 *       and attaches it via {@code defaultOptions(...)}. That exercises the plugin's
 *       provider-specific ChatOptions pass-through: every field survives the round-trip across the
 *       activity boundary.
 * </ul>
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

    // Create a chat client using Anthropic Claude. The per-model ActivityOptions (longer
    // start-to-close + schedule-to-close caps) live on the ChatModelActivityOptions bean in
    // ChatModelConfig; forModel(name) consults that bean automatically. The workflow stays
    // free of infrastructure wiring — ideal for static per-model config.
    ActivityChatModel anthropicModel = ActivityChatModel.forModel("anthropicChatModel");
    chatClients.put(
        "anthropic",
        TemporalChatClient.builder(anthropicModel)
            .defaultSystem(
                "You are a helpful assistant powered by Anthropic. "
                    + "You excel at careful reasoning and nuanced responses.")
            .build());

    // Create a chat client that turns on Anthropic's extended-thinking mode. This exercises
    // the plugin's provider-specific ChatOptions pass-through end to end: the
    // AnthropicChatOptions (with thinking=ENABLED + budget_tokens) is passed via
    // .defaultOptions(...) on the ChatClient, crosses the activity boundary serialized as
    // (class name, JSON), and is rehydrated by ChatModelActivityImpl before the prompt is
    // sent to Claude. Required side effects for extended thinking: temperature must be 1.0
    // and max_tokens must exceed budget_tokens.
    // @@@SNIPSTART samples-java-spring-ai-provider-options
    AnthropicChatOptions thinkingOptions =
        AnthropicChatOptions.builder()
            .thinking(AnthropicApi.ThinkingType.ENABLED, 1024)
            .temperature(1.0)
            .maxTokens(4096)
            .build();
    chatClients.put(
        "think",
        TemporalChatClient.builder(anthropicModel)
            .defaultSystem(
                "You are a helpful assistant powered by Anthropic with extended thinking. "
                    + "Use the thinking budget to reason carefully, then give a crisp answer "
                    + "that reflects the reasoning you did.")
            .defaultOptions(thinkingOptions)
            .build());
    // @@@SNIPEND
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
