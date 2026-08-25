package io.temporal.samples.cloudrunworkerid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Activity implementation that returns a simple greeting. */
public final class GreetingActivitiesImpl implements GreetingActivities {

  private static final Logger logger = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

  @Override
  public String composeGreeting(String name) {
    logger.info("Composing greeting for {}", name);
    return "Hello, " + name + "!";
  }
}
