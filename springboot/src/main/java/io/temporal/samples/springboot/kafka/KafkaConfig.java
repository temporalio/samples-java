

package io.temporal.samples.springboot.kafka;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Configuration()
@Profile("!test")
public class KafkaConfig {
  @Value(value = "${spring.kafka.bootstrap-servers}")
  private String bootstrapAddress;

  @Value(value = "${samples.message.topic.name}")
  private String topicName;

  @Autowired private MessageController messageController;

  @Bean
  EmbeddedKafkaBroker broker() {
    return new EmbeddedKafkaBroker(1)
        .kafkaPorts(9092)
        .brokerListProperty("spring.kafka.bootstrap-servers");
  }

  @Bean
  public KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopic samplesTopic() {
    return new NewTopic(topicName, 1, (short) 1);
  }

  @KafkaListener(id = "samples-topic", topics = "samples-topic")
  public void kafkaListener(String message) {
    SseEmitter latestEm = messageController.getLatestEmitter();

    try {
      latestEm.send(message);
    } catch (IOException e) {
      latestEm.completeWithError(e);
    }
  }
}
