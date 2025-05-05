

package io.temporal.samples.tracing.workflow;

public class TracingActivitiesImpl implements TracingActivities {
  @Override
  public String greet(String name, String language) {
    String greeting;

    switch (language) {
      case "Spanish":
        greeting = "Hola " + name;
        break;
      case "French":
        greeting = "Bonjour " + name;
        break;
      default:
        greeting = "Hello " + name;
    }

    return greeting;
  }
}
