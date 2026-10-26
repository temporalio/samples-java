package io.temporal.samples.springai.multimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ChatModelConfigTest {

  @Test
  void providerBeansInitializeWithoutNetworkCalls() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "test",
                  Map.of(
                      "spring.ai.openai.api-key", "test-openai-key",
                      "spring.ai.anthropic.api-key", "test-anthropic-key")));
      context.register(ChatModelConfig.class);
      context.refresh();

      var openAi = assertInstanceOf(OpenAiChatModel.class, context.getBean("openAiChatModel"));
      var anthropic =
          assertInstanceOf(AnthropicChatModel.class, context.getBean("anthropicChatModel"));
      assertSame(openAi, context.getBean(ChatModel.class));
      assertEquals(0, openAi.getOptions().getMaxRetries());
      assertEquals(0, anthropic.getOptions().getMaxRetries());
    }
  }
}
