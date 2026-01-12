package io.temporal.samples.temporalcloudopenmetrics.activities;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;

public class ScenarioActivitiesImpl implements ScenarioActivities {

  @Override
  public String doWork(String name, String scenario) {
    if ("fail".equalsIgnoreCase(scenario)) {
      throw ApplicationFailure.newNonRetryableFailure(
          "Intentional failure for dashboard", "SCENARIO_FAIL");
    }

    if ("timeout".equalsIgnoreCase(scenario)) {
      sleepMs(5_000);
      return "unreachable (should have timed out)";
    }

    if ("cancel".equalsIgnoreCase(scenario)) {
      while (true) {
        Activity.getExecutionContext().heartbeat("still-running");
        sleepMs(1_000);
      }
    }

    // success
    return "Hello " + name;
  }

  private static void sleepMs(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted", e);
    }
  }
}
