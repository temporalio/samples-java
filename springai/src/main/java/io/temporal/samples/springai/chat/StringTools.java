package io.temporal.samples.springai.chat;

import io.temporal.springai.tool.DeterministicTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Deterministic string manipulation tools.
 *
 * <p>This class demonstrates the use of {@link DeterministicTool} annotation for tools that are
 * safe to execute directly in a Temporal workflow without wrapping in an activity.
 *
 * <p>Deterministic tools must:
 *
 * <ul>
 *   <li>Always produce the same output for the same input
 *   <li>Have no side effects (no I/O, no random numbers, no system time)
 *   <li>Not call any non-deterministic APIs
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StringTools stringTools = new StringTools();
 * this.chatClient = TemporalChatClient.builder(activityChatModel)
 *         .defaultTools(stringTools)  // Executes directly in workflow
 *         .build();
 * }</pre>
 */
@DeterministicTool
public class StringTools {

  /**
   * Reverses a string.
   *
   * @param input the string to reverse
   * @return the reversed string
   */
  @Tool(description = "Reverse a string, returning the characters in opposite order")
  public String reverse(@ToolParam(description = "The string to reverse") String input) {
    if (input == null) {
      return null;
    }
    return new StringBuilder(input).reverse().toString();
  }

  /**
   * Counts the number of words in a text.
   *
   * @param text the text to count words in
   * @return the word count
   */
  @Tool(description = "Count the number of words in a text")
  public int countWords(@ToolParam(description = "The text to count words in") String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return text.trim().split("\\s+").length;
  }

  /**
   * Converts text to uppercase.
   *
   * @param text the text to convert
   * @return the uppercase text
   */
  @Tool(description = "Convert text to all uppercase letters")
  public String toUpperCase(@ToolParam(description = "The text to convert") String text) {
    if (text == null) {
      return null;
    }
    return text.toUpperCase(java.util.Locale.ROOT);
  }

  /**
   * Converts text to lowercase.
   *
   * @param text the text to convert
   * @return the lowercase text
   */
  @Tool(description = "Convert text to all lowercase letters")
  public String toLowerCase(@ToolParam(description = "The text to convert") String text) {
    if (text == null) {
      return null;
    }
    return text.toLowerCase(java.util.Locale.ROOT);
  }

  /**
   * Checks if a string is a palindrome.
   *
   * @param text the text to check
   * @return true if the text is a palindrome (ignoring case and spaces)
   */
  @Tool(description = "Check if a string is a palindrome (reads the same forwards and backwards)")
  public boolean isPalindrome(@ToolParam(description = "The text to check") String text) {
    if (text == null) {
      return false;
    }
    String normalized = text.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
    String reversed = new StringBuilder(normalized).reverse().toString();
    return normalized.equals(reversed);
  }
}
