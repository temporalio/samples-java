package io.temporal.samples.springboot.kafka;

import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "KafkaSampleTaskQueue")
public class KafkaActivityImpl implements KafkaActivity {

  // Setting required to false means we won't fail
  // if a test does not have kafka enabled
  @Autowired(required = false)
  private KafkaTemplate<String, String> kafkaTemplate;

  @Value(value = "${samples.message.topic.name}")
  private String topicName;

  @Override
  public void sendMessage(String message) {
    try {
      kafkaTemplate.send(topicName, message).get();
    } catch (Exception e) {
      throw ApplicationFailure.newFailure(
          "Unable to send message.", e.getClass().getName(), e.getMessage());
    }
  }
}
