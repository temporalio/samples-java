package io.temporal.samples.gcp.cloudrun.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GreetingActivitiesImpl implements GreetingActivities {

  private static final Logger logger = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

  @Override
  public String composeGreeting(String name) {
    logger.info("Composing greeting for {}", name);
    return "Hello, " + name + "!";
  }
}
