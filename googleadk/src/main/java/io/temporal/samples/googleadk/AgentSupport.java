package io.temporal.samples.googleadk;

import com.google.adk.events.Event;
import com.google.genai.types.Content;
import java.util.List;

/** Shared names and a deterministic helper for reading an agent's final answer. */
public final class AgentSupport {
  public static final String MODEL = "gemini-2.5-flash";
  public static final String TASK_QUEUE = "google-adk-samples";
  public static final String USER = "user-1";
  public static final String SESSION = "session-1";

  private AgentSupport() {}

  public static String lastText(List<Event> events) {
    for (int i = events.size() - 1; i >= 0; i--) {
      String text = events.get(i).content().map(Content::text).orElse("");
      if (text != null && !text.isEmpty()) {
        return text;
      }
    }
    return "";
  }
}
