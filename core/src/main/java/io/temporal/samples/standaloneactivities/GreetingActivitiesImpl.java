package io.temporal.samples.standaloneactivities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Activity implementation. */
public class GreetingActivitiesImpl implements GreetingActivities {

  private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

  @Override
  public String composeGreeting(String greeting, String name) {
    log.info("Composing greeting...");
    return greeting + ", " + name + "!";
  }
}
