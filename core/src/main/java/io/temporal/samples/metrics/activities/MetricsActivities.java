

package io.temporal.samples.metrics.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface MetricsActivities {
  String performA(String input);

  String performB(String input);
}
