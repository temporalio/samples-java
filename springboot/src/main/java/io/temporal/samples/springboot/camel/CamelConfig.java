

package io.temporal.samples.springboot.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration()
@Profile("!test")
public class CamelConfig {
  @Autowired private CamelContext camelContext;

  @Bean
  ServletRegistrationBean servletRegistrationBean() {
    String contextPath = "/temporalapp";
    ServletRegistrationBean servlet =
        new ServletRegistrationBean(new CamelHttpTransportServlet(), contextPath + "/*");
    servlet.setName("CamelServlet");
    return servlet;
  }

  @Bean
  ProducerTemplate producerTemplate() {
    return camelContext.createProducerTemplate();
  }

  @Bean
  ConsumerTemplate consumerTemplate() {
    return camelContext.createConsumerTemplate();
  }
}
