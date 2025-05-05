

package io.temporal.samples.customchangeversion;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CustomChangeVersionActivities {
  String customOne(String input);

  String customTwo(String input);

  String customThree(String input);
}
