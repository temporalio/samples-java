package io.temporal.samples.customchangeversion;

public class CustomChangeVersionActivitiesImpl implements CustomChangeVersionActivities {
  @Override
  public String customOne(String input) {
    return "\ncustomOne activity - " + input;
  }

  @Override
  public String customTwo(String input) {
    return "\ncustomTwo activity - " + input;
  }

  @Override
  public String customThree(String input) {
    return "\ncustomThree activity - " + input;
  }
}
