

package io.temporal.samples.springboot;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.springboot.camel.OfficeOrder;
import io.temporal.samples.springboot.camel.OrderWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest(classes = HelloSampleTest.Configuration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// set this to omit setting up embedded kafka
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
public class CamelSampleTest {

  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

  @BeforeEach
  void setUp() {
    applicationContext.start();
  }

  @Test
  public void testOrdersWorkflow() {
    OrderWorkflow workflow =
        workflowClient.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("CamelSampleTaskQueue")
                .setWorkflowId("CamelSampleWorkflow")
                .build());
    List<OfficeOrder> result = workflow.start();
    Assert.notNull(result, "Result should not be null");
    Assert.isTrue(result.size() == 9, "Invalid result");
  }
}
