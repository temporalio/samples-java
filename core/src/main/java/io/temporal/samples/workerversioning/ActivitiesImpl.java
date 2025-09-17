package io.temporal.samples.workerversioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivitiesImpl implements Activities {

  private static final Logger logger = LoggerFactory.getLogger(ActivitiesImpl.class);

  @Override
  public String someActivity(String calledBy) {
    logger.info("SomeActivity called by {}", calledBy);
    return "SomeActivity called by " + calledBy;
  }

  @Override
  public String someIncompatibleActivity(IncompatibleActivityInput input) {
    logger.info(
        "SomeIncompatibleActivity called by {} with {}", input.getCalledBy(), input.getMoreData());
    return "SomeIncompatibleActivity called by "
        + input.getCalledBy()
        + " with "
        + input.getMoreData();
  }
}
