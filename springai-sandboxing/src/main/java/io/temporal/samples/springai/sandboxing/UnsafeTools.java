package io.temporal.samples.springai.sandboxing;

import org.springframework.ai.tool.annotation.Tool;

/**
 * Example of tools that are NOT properly annotated for workflow safety.
 *
 * <p>This class demonstrates what happens when you pass tools to {@code TemporalChatClient} without
 * the proper {@code @DeterministicTool} or {@code @SideEffectTool} annotations.
 *
 * <p><b>Without sandboxing:</b> Passing this class to {@code .defaultTools()} will throw an {@link
 * IllegalArgumentException} because the framework cannot verify the tools are workflow-safe.
 *
 * <p><b>With sandboxing:</b> When {@link io.temporal.springai.advisor.SandboxingAdvisor} is used,
 * these tools are automatically wrapped in local activities with a warning. This ensures
 * deterministic replay but adds overhead.
 *
 * <h2>Why This Is "Unsafe"</h2>
 *
 * <p>These tools have several problems:
 *
 * <ul>
 *   <li>{@code currentTime()} - Returns different values on replay (non-deterministic)
 *   <li>{@code getSystemProperty()} - Depends on system environment (non-deterministic)
 *   <li>{@code randomNumber()} - Returns random values (non-deterministic)
 * </ul>
 *
 * <h2>How To Fix</h2>
 *
 * <p>If you see warnings about your tools being wrapped in local activities, you have three
 * options:
 *
 * <ol>
 *   <li>Add {@code @DeterministicTool} if the tool is truly deterministic (same output for same
 *       input, no side effects)
 *   <li>Add {@code @SideEffectTool} if the tool is non-deterministic but doesn't need activity
 *       durability (timestamps, UUIDs, random values)
 *   <li>Convert the tool to a Temporal activity for full durability and retry support
 * </ol>
 *
 * @see io.temporal.springai.tool.DeterministicTool
 * @see io.temporal.springai.tool.SideEffectTool
 */
public class UnsafeTools {

  /**
   * Gets the current time in milliseconds.
   *
   * <p><b>Problem:</b> Returns different values on workflow replay, breaking determinism.
   *
   * <p><b>Fix:</b> Annotate the class with {@code @SideEffectTool} to wrap in {@code
   * Workflow.sideEffect()}.
   *
   * @return current time in milliseconds since epoch
   */
  @Tool(description = "Get the current time in milliseconds since epoch")
  public long currentTime() {
    return System.currentTimeMillis();
  }

  /**
   * Gets a system property value.
   *
   * <p><b>Problem:</b> System properties can differ between workers or change over time, leading to
   * non-deterministic behavior on replay.
   *
   * <p><b>Fix:</b> Either read system properties at workflow start and pass as state, or use an
   * activity to read system properties.
   *
   * @param name the property name
   * @return the property value, or "not set" if not found
   */
  @Tool(description = "Get a system property value")
  public String getSystemProperty(String name) {
    return System.getProperty(name, "not set");
  }

  /**
   * Generates a random number.
   *
   * <p><b>Problem:</b> Returns different values on replay, breaking determinism.
   *
   * <p><b>Fix:</b> Annotate the class with {@code @SideEffectTool} to wrap in {@code
   * Workflow.sideEffect()}.
   *
   * @param max the maximum value (exclusive)
   * @return a random number between 0 and max
   */
  @Tool(description = "Generate a random number between 0 and the given maximum (exclusive)")
  public int randomNumber(int max) {
    return (int) (Math.random() * max);
  }
}
