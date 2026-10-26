package io.temporal.samples.googleadk.chat;

import com.google.adk.sessions.Session;
import java.util.List;

/** Session and queued messages carried into the next Run after Continue-As-New. */
public record ChatInput(
    Session session, List<String> messages, boolean finished, String lastReply) {
  public static ChatInput empty() {
    return new ChatInput(null, List.of(), false, "");
  }
}
