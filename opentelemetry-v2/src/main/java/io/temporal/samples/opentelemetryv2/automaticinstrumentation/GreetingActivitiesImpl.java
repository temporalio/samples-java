package io.temporal.samples.opentelemetryv2.automaticinstrumentation;

public class GreetingActivitiesImpl implements GreetingActivities {
  @Override
  public String composeGreeting(String name) {
    return "Hello, " + name + "!";
  }
}
