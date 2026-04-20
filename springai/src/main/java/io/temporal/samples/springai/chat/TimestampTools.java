package io.temporal.samples.springai.chat;

import io.temporal.springai.tool.SideEffectTool;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Side-effect tools that return non-deterministic values.
 *
 * <p>This class demonstrates the use of {@link SideEffectTool} annotation for tools that are
 * non-deterministic but don't need the full durability of an activity.
 *
 * <p>Side-effect tools are wrapped in {@code Workflow.sideEffect()}, which:
 *
 * <ul>
 *   <li>Records the result in workflow history on first execution
 *   <li>Returns the recorded result on replay (deterministic)
 *   <li>Does not create activity tasks (lightweight)
 * </ul>
 *
 * <p>Use {@code @SideEffectTool} for:
 *
 * <ul>
 *   <li>Getting current time/date
 *   <li>Generating random values (UUIDs, random numbers)
 *   <li>Any non-deterministic operation that doesn't need retry/durability
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TimestampTools timestampTools = new TimestampTools();
 * this.chatClient = TemporalChatClient.builder(activityChatModel)
 *         .defaultTools(timestampTools)  // Wrapped in sideEffect()
 *         .build();
 * }</pre>
 */
@SideEffectTool
public class TimestampTools {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

  /**
   * Gets the current date and time.
   *
   * <p>This is non-deterministic (returns different values each time), but wrapped in sideEffect()
   * it becomes safe for workflow replay.
   *
   * @return the current date and time as a formatted string
   */
  @Tool(description = "Get the current date and time")
  public String getCurrentDateTime() {
    return FORMATTER.format(Instant.now());
  }

  /**
   * Gets the current Unix timestamp in milliseconds.
   *
   * @return the current time in milliseconds since epoch
   */
  @Tool(description = "Get the current Unix timestamp in milliseconds")
  public long getCurrentTimestamp() {
    return System.currentTimeMillis();
  }

  /**
   * Generates a random UUID.
   *
   * @return a new random UUID string
   */
  @Tool(description = "Generate a random UUID")
  public String generateUuid() {
    return UUID.randomUUID().toString();
  }

  /**
   * Gets the current date and time in a specific timezone.
   *
   * @param timezone the timezone ID (e.g., "America/New_York", "UTC", "Europe/London")
   * @return the current date and time in the specified timezone
   */
  @Tool(description = "Get the current date and time in a specific timezone")
  public String getDateTimeInTimezone(
      @ToolParam(description = "Timezone ID (e.g., 'America/New_York', 'UTC', 'Europe/London')")
          String timezone) {
    try {
      ZoneId zoneId = ZoneId.of(timezone);
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(zoneId);
      return formatter.format(Instant.now());
    } catch (Exception e) {
      return "Invalid timezone: " + timezone + ". Use formats like 'America/New_York' or 'UTC'.";
    }
  }
}
