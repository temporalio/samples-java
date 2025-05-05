

package io.temporal.samples.tracing.workflow;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface TracingActivities {
  String greet(String name, String language);
}
