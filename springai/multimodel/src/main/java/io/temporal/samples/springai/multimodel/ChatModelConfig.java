package io.temporal.samples.springai.multimodel;

import io.temporal.activity.ActivityOptions;
import io.temporal.springai.autoconfigure.ChatModelActivityOptions;
import io.temporal.springai.model.ActivityChatModel;
import java.time.Duration;
import java.util.Map;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for multiple chat models from different providers.
 *
 * <p>This demonstrates how to configure multiple AI providers in a Spring Boot application. Each
 * model is registered as a separate bean with a unique name.
 *
 * <p>In workflows, these can be accessed via:
 *
 * <ul>
 *   <li>{@code ActivityChatModel.forDefault()} - Uses the @Primary model (openAiChatModel)
 *   <li>{@code ActivityChatModel.forModel("openAiChatModel")} - Uses OpenAI gpt-4o-mini
 *   <li>{@code ActivityChatModel.forModel("anthropicChatModel")} - Uses Anthropic Claude
 * </ul>
 */
@Configuration
public class ChatModelConfig {

  @Value("${spring.ai.openai.api-key}")
  private String openAiApiKey;

  @Value("${spring.ai.anthropic.api-key}")
  private String anthropicApiKey;

  /**
   * OpenAI model using gpt-4o-mini for quick, cost-effective responses. Marked as @Primary so it's
   * used when no specific model is requested.
   */
  @Bean
  @Primary
  public ChatModel openAiChatModel() {
    OpenAiApi api = OpenAiApi.builder().apiKey(openAiApiKey).build();
    OpenAiChatOptions options =
        OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.7).build();
    return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
  }

  /** Anthropic model using Claude for complex reasoning tasks. */
  @Bean
  public ChatModel anthropicChatModel() {
    AnthropicApi api = AnthropicApi.builder().apiKey(anthropicApiKey).build();
    AnthropicChatOptions options =
        AnthropicChatOptions.builder()
            .model("claude-sonnet-4-20250514")
            .temperature(0.3) // Lower temperature for more focused reasoning
            .build();
    return AnthropicChatModel.builder().anthropicApi(api).defaultOptions(options).build();
  }

  /**
   * Per-model {@link ActivityOptions} overrides, declared as a single Spring bean. When present,
   * {@link ActivityChatModel#forModel(String)} and {@link ActivityChatModel#forDefault()} consult
   * this map before falling back to the plugin's defaults — so workflows can build a
   * fully-configured chat model with nothing more than {@code ActivityChatModel.forModel(name)}.
   *
   * <p>The Anthropic entry bumps the start-to-close timeout (reasoning models can take minutes) and
   * caps the schedule-to-close so a stuck request can't keep re-attempting forever. Building on
   * {@link ActivityChatModel#defaultActivityOptions()} preserves the plugin's
   * non-retryable-AI-error classification without having to restate it.
   *
   * <p>The workflow still uses the per-call {@code ChatClient.defaultOptions(...)} path for things
   * that change per prompt (see the {@code think:} route in {@code MultiModelWorkflowImpl} —
   * extended thinking is enabled per call, not globally).
   */
  // @@@SNIPSTART samples-java-spring-ai-per-model-options
  @Bean
  public ChatModelActivityOptions chatModelActivityOptions() {
    return new ChatModelActivityOptions(
        Map.of(
            "anthropicChatModel",
            ActivityOptions.newBuilder(ActivityChatModel.defaultActivityOptions())
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setScheduleToCloseTimeout(Duration.ofMinutes(15))
                .build()));
  }
  // @@@SNIPEND
}
