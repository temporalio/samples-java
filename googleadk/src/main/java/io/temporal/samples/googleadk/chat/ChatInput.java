package io.temporal.samples.googleadk.chat;

import com.google.adk.sessions.Session;

public record ChatInput(Session session, String lastReply) {
  public static ChatInput empty() {
    return new ChatInput(null, "");
  }
}
