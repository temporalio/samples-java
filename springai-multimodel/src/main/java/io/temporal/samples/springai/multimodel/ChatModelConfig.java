package io.temporal.samples.springai.multimodel;

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
}
