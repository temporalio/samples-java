package io.temporal.samples.nexusstandalone.service;

// A helper method to generate workflow IDs.
public final class GreetingIds {
  private GreetingIds() {}

  public static String backingWorkflowId(String name) {
    return "greeting-" + name;
  }
}
