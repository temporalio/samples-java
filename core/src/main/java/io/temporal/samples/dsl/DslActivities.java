package io.temporal.samples.dsl;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface DslActivities {
  String one();

  String two();

  String three();

  String four();
}
