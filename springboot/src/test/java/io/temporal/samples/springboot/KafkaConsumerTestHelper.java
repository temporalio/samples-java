package io.temporal.samples.springboot;

import java.util.concurrent.CountDownLatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerTestHelper {
  private CountDownLatch latch = new CountDownLatch(5);
  private String payload = null;

  @KafkaListener(id = "samples-test-id", topics = "${samples.message.topic.name}")
  public void receive(ConsumerRecord<?, ?> consumerRecord) {

    setPayload(consumerRecord.toString());
    latch.countDown();
  }

  public CountDownLatch getLatch() {
    return latch;
  }

  public String getPayload() {
    return payload;
  }

  private void setPayload(String payload) {
    this.payload = payload;
  }
}
