package io.temporal.samples.lambdaworker;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Activity implementation that returns a simple greeting. */
public class GreetingActivitiesImpl implements GreetingActivities {

  private static final Logger logger = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

  @Override
  public String createGreeting(String name) {
    ActivityInfo info = Activity.getExecutionContext().getInfo();
    logger.info(
        "Running activity {} for workflow {}", info.getActivityType(), info.getWorkflowId());
    return "Hello, " + name + "!";
  }
}
