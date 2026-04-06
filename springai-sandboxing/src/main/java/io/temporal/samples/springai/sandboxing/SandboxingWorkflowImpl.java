package io.temporal.samples.springai.sandboxing;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.springai.activity.ChatModelActivity;
import io.temporal.springai.advisor.SandboxingAdvisor;
import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;

/**
 * Demonstrates sandboxing mode for unsafe tools.
 *
 * <p>This workflow shows how to use {@link SandboxingAdvisor} to safely use tools that are not
 * properly annotated with {@code @DeterministicTool} or {@code @SideEffectTool}.
 *
 * <h2>Two Sandboxing Approaches</h2>
 *
 * <p>There are two ways to enable sandboxing:
 *
 * <ol>
 *   <li><b>SandboxingAdvisor</b> (recommended) - Wraps tools at call time:
 *       <pre>{@code
 * .defaultAdvisors(new SandboxingAdvisor())
 * .defaultToolCallbacks(ToolCallbacks.from(unsafeTools))
 *
 * }</pre>
 *   <li><b>Builder option</b> - Wraps tools at registration time:
 *       <pre>{@code
 * .defaultAdvisors(new SandboxingAdvisor())
 * .defaultTools(unsafeTools)
 *
 * }</pre>
 * </ol>
 *
 * <p>This example uses SandboxingAdvisor, which matches the original design.
 *
 * <h2>What This Example Demonstrates</h2>
 *
 * <p>When running this example, you will see warning messages like:
 *
 * <pre>
 * WARN io.temporal.springai.advisor.SandboxingAdvisor - Tool 'currentTime'
 *   (org.springframework.ai.tool.method.MethodToolCallback) is not guaranteed
 *   to be deterministic. Wrapping in local activity for workflow safety.
 * </pre>
 *
 * <p>Despite the warnings, the tools will work correctly because they are wrapped in local
 * activities. However, this adds overhead. The warnings help developers identify tools that should
 * be properly annotated.
 *
 * <h2>Viewing in Temporal UI</h2>
 *
 * <p>When you call a tool like "What time is it?", you'll see:
 *
 * <ol>
 *   <li>A {@code ChatModelActivity} task for the AI model call
 *   <li>A local activity marker for the sandboxed tool call
 * </ol>
 *
 * <p>Compare this to the main example where {@code @SideEffectTool} is used - there you'll see a
 * sideEffect marker instead of a local activity, which is more lightweight.
 *
 * @see UnsafeTools
 * @see SandboxingAdvisor
 */
public class SandboxingWorkflowImpl implements SandboxingWorkflow {

  private final ChatClient chatClient;
  private boolean ended = false;
  private int messageCount = 0;

  @WorkflowInit
  public SandboxingWorkflowImpl(String systemPrompt) {
    // Create an activity stub for calling the AI model
    ChatModelActivity chatModelActivity =
        Workflow.newActivityStub(
            ChatModelActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());

    // Wrap the activity in ActivityChatModel to use with Spring AI
    ActivityChatModel activityChatModel = new ActivityChatModel(chatModelActivity);

    // Create unsafe tools - NOT annotated with @DeterministicTool or @SideEffectTool
    UnsafeTools unsafeTools = new UnsafeTools();

    // Build a TemporalChatClient with SandboxingAdvisor
    //
    // The SandboxingAdvisor intercepts chat requests and wraps any tool callbacks
    // that aren't already safe (ActivityToolCallback, SideEffectToolCallback) in
    // LocalActivityToolCallbackWrapper.
    //
    // Note: We use defaultToolCallbacks() with ToolCallbacks.from() instead of
    // defaultTools() because defaultTools() would reject the unannotated tools.
    // The SandboxingAdvisor handles the wrapping at call time instead.
    this.chatClient =
        TemporalChatClient.builder(activityChatModel)
            .defaultAdvisors(new SandboxingAdvisor()) // <-- Wraps unsafe tools!
            .defaultSystem(systemPrompt)
            .defaultToolCallbacks(ToolCallbacks.from(unsafeTools)) // Pass as raw callbacks
            .build();
  }

  @Override
  public String run(String systemPrompt) {
    // Wait until the chat is ended
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
