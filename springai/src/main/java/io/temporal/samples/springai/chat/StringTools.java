package io.temporal.samples.springai.chat;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Deterministic string manipulation tools.
 *
 * <p>These tools execute directly in workflow context. Since they are pure functions (same output
 * for same input, no side effects), they are safe for Temporal replay without any wrapping.
 */
// @@@SNIPSTART samples-java-spring-ai-plain-tool
public class StringTools {

  @Tool(description = "Reverse a string, returning the characters in opposite order")
  public String reverse(@ToolParam(description = "The string to reverse") String input) {
    if (input == null) {
      return null;
    }
    return new StringBuilder(input).reverse().toString();
  }

  @Tool(description = "Count the number of words in a text")
  public int countWords(@ToolParam(description = "The text to count words in") String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return text.trim().split("\\s+").length;
  }

  @Tool(description = "Convert text to all uppercase letters")
  public String toUpperCase(@ToolParam(description = "The text to convert") String text) {
    if (text == null) {
      return null;
    }
    return text.toUpperCase(java.util.Locale.ROOT);
  }

  @Tool(description = "Convert text to all lowercase letters")
  public String toLowerCase(@ToolParam(description = "The text to convert") String text) {
    if (text == null) {
      return null;
    }
    return text.toLowerCase(java.util.Locale.ROOT);
  }

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
// @@@SNIPEND
