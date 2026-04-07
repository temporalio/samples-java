package io.temporal.samples.temporalmetricsdemo.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ScenarioActivities {
  @ActivityMethod
  String doWork(String name, String scenario);
}
