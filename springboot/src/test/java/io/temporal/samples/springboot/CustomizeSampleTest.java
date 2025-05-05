

package io.temporal.samples.springboot;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.springboot.customize.CustomizeWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest(classes = CustomizeSampleTest.Configuration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// set this to omit setting up embedded kafka
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@DirtiesContext
public class CustomizeSampleTest {
  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

  @BeforeEach
  void setUp() {
    applicationContext.start();
  }

  @Test
  public void testHello() {
    CustomizeWorkflow workflow =
        workflowClient.newWorkflowStub(
            CustomizeWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("CustomizeTaskQueue")
                .setWorkflowId("CustomizeSampleTest")
                .build());
    String result = workflow.execute();
    Assert.notNull(result, "Result should not be null");
    Assert.isTrue(result.equals("Completed as Local activity!"), "Invalid result");
  }

  @ComponentScan
  public static class Configuration {}
}
