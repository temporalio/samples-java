package io.temporal.samples.springboot.kafka;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface KafkaActivity {
  void sendMessage(String message);
}
