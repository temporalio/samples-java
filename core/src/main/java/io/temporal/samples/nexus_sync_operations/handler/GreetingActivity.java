package io.temporal.samples.nexus_sync_operations.handler;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.samples.nexus_sync_operations.service.Language;

@ActivityInterface
public interface GreetingActivity {
  // Simulates a call to a remote greeting service. Returns null if the language is not supported.
  @ActivityMethod
  String callGreetingService(Language language);
}
