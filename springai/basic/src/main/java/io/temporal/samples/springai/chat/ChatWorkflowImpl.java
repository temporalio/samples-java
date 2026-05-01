package io.temporal.samples.springai.chat;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

/**
 * Implementation of the chat workflow using Spring AI's ChatClient with Temporal tools.
 *
 * <p>This demonstrates how to use the Spring AI plugin within a Temporal workflow:
 *
 * <ol>
 *   <li>Build an {@link ActivityChatModel} via its factory to get a standard Spring AI ChatModel
 *       backed by a durable Temporal activity
 *   <li>Create activity stubs for tools (e.g., {@link WeatherActivity})
 *   <li>Create deterministic tools (e.g., {@link StringTools})
 *   <li>Create side-effect tools (e.g., {@link TimestampTools})
 *   <li>Use {@link TemporalChatClient} to build a tool-aware chat client
 * </ol>
 *
 * <p>The AI model can call:
 *
 * <ul>
 *   <li>{@code getWeather(city)} - Executes as a durable Temporal activity
 *   <li>{@code getForecast(city, days)} - Executes as a durable Temporal activity
 *   <li>{@code reverse(text)}, {@code countWords(text)}, etc. - Execute directly in workflow (plain
 *       workflow tool)
 *   <li>{@code getCurrentDateTime()}, {@code generateUuid()}, etc. - Wrapped in sideEffect
 *       (@SideEffectTool)
 * </ul>
 */
public class ChatWorkflowImpl implements ChatWorkflow {

  private final ChatClient chatClient;
  private boolean ended = false;
  private int messageCount = 0;

  // @@@SNIPSTART samples-java-spring-ai-chat-workflow-init
  @WorkflowInit
  public ChatWorkflowImpl(String systemPrompt) {
    // Build an activity-backed chat model. The factory creates the activity stub
    // internally and registers per-call Summaries on the Temporal UI.
    ActivityChatModel activityChatModel = ActivityChatModel.forDefault();

    // Create an activity stub for weather tools - these execute as durable activities
    WeatherActivity weatherTool =
        Workflow.newActivityStub(
            WeatherActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());

    // Create deterministic tools - these execute directly in the workflow
    StringTools stringTools = new StringTools();

    // Create side-effect tools - these are wrapped in Workflow.sideEffect()
    // The result is recorded in history, making replay deterministic
    TimestampTools timestampTools = new TimestampTools();

    // Create chat memory - uses in-memory storage that gets rebuilt on replay
    ChatMemory chatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(20)
            .build();

    // Build a TemporalChatClient with tools and memory
    // - Activity stubs (weatherTool) become durable AI tools
    // - plain workflow tool classes (stringTools) execute directly in workflow
    // - @SideEffectTool classes (timestampTools) are wrapped in sideEffect()
    // - PromptChatMemoryAdvisor maintains conversation history
    this.chatClient =
        TemporalChatClient.builder(activityChatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(weatherTool, stringTools, timestampTools)
            .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build())
            .build();
  }

  // @@@SNIPEND

  @Override
  public String run(String systemPrompt) {
    // systemPrompt is unused here on purpose — @WorkflowInit requires the constructor
    // and the @WorkflowMethod to share a parameter list, and the constructor above
    // already consumed it to build the chat client.
    Workflow.await(() -> ended);
    return "Chat ended after " + messageCount + " messages.";
  }

  @Override
  public String chat(String message) {
    messageCount++;
    return chatClient.prompt().user(message).call().content();
  }

  @Override
  public void end() {
    ended = true;
  }
}
