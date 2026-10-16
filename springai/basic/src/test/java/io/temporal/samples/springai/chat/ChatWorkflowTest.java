package io.temporal.samples.springai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.springai.activity.ChatModelActivityImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class ChatWorkflowTest {

  @Test
  void toolsAndConversationMemorySurviveReplay() throws Exception {
    List<Prompt> requests = new CopyOnWriteArrayList<>();
    ChatModel model =
        prompt -> {
          requests.add(prompt);
          AssistantMessage response;
          if (requests.size() == 1) {
            response =
                AssistantMessage.builder()
                    .toolCalls(
                        List.of(
                            new AssistantMessage.ToolCall(
                                "weather-1", "function", "getWeather", "{\"city\":\"Seattle\"}")))
                    .build();
          } else {
            response =
                new AssistantMessage(
                    requests.size() == 2 ? "Seattle is rainy." : "You asked about Seattle.");
          }
          return new ChatResponse(List.of(new Generation(response)));
        };

    try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
      var worker = env.newWorker("spring-ai-chat-test");
      worker.registerWorkflowImplementationTypes(ChatWorkflowImpl.class);
      worker.registerActivitiesImplementations(
          new ChatModelActivityImpl(model), new WeatherActivityImpl());
      env.start();

      ChatWorkflow workflow =
          env.getWorkflowClient()
              .newWorkflowStub(
                  ChatWorkflow.class,
                  WorkflowOptions.newBuilder()
                      .setTaskQueue("spring-ai-chat-test")
                      .setWorkflowExecutionTimeout(Duration.ofSeconds(20))
                      .build());
      WorkflowClient.start(workflow::run, "Answer weather questions.");
      assertEquals("Seattle is rainy.", workflow.chat("What's the weather in Seattle?"));
      assertEquals("You asked about Seattle.", workflow.chat("Which city did I ask about?"));
      workflow.end();
      WorkflowStub stub = WorkflowStub.fromTyped(workflow);
      assertEquals("Chat ended after 2 messages.", stub.getResult(String.class));

      assertEquals(3, requests.size());
      assertTrue(
          requests.get(1).getInstructions().stream()
              .filter(ToolResponseMessage.class::isInstance)
              .map(ToolResponseMessage.class::cast)
              .flatMap(message -> message.getResponses().stream())
              .anyMatch(response -> response.responseData().contains("Seattle")));
      assertTrue(
          requests.get(2).getInstructions().stream()
              .anyMatch(message -> "What's the weather in Seattle?".equals(message.getText())));
      assertTrue(
          requests.get(2).getInstructions().stream()
              .anyMatch(message -> "Seattle is rainy.".equals(message.getText())));

      WorkflowReplayer.replayWorkflowExecution(
          env.getWorkflowClient().fetchHistory(stub.getExecution().getWorkflowId()),
          ChatWorkflowImpl.class);
    }
  }
}
