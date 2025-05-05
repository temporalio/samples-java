

package io.temporal.samples.polling;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PollingActivities {
  String doPoll();
}
