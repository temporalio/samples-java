package io.temporal.samples.springboot.customize;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CustomizeActivity {
  String run(String input);
}
