

package io.temporal.samples.springboot.camel;

import java.util.List;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CamelResource {
  @Autowired private ProducerTemplate producerTemplate;

  @GetMapping("/orders")
  @ResponseBody
  public List<OfficeOrder> getProductsByCategory() {
    producerTemplate.start();
    List<OfficeOrder> orders = producerTemplate.requestBody("direct:getOrders", null, List.class);
    producerTemplate.stop();
    return orders;
  }
}
