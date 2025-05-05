package io.temporal.samples.sleepfordays;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SendEmailActivity {
  void sendEmail(String email);
}
