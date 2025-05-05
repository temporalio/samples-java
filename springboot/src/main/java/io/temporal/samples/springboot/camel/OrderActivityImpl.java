package io.temporal.samples.springboot.camel;

import io.temporal.spring.boot.ActivityImpl;
import java.util.List;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "CamelSampleTaskQueue")
public class OrderActivityImpl implements OrderActivity {

  @Autowired private ProducerTemplate producerTemplate;

  @Override
  public List<OfficeOrder> getOrders() {
    producerTemplate.start();
    List<OfficeOrder> orders =
        producerTemplate.requestBody("direct:findAllOrders", null, List.class);
    producerTemplate.stop();
    return orders;
  }
}
