package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.basic.BasicWorkflow;
import io.temporal.samples.googleadk.basic.BasicWorkflowImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class BasicWorkflowTest {
  @Test
  void modelCallIsAnActivityAndReplaysWithoutCallingTheModel() throws Exception {
    // @@@SNIPSTART java-google-adk-basic-test
    ScriptedModel model =
        ScriptedModel.of(AgentSupport.MODEL, ScriptedModel.text("Hello from ADK"));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, BasicWorkflowImpl.class)) {
      test.environment.start();
      BasicWorkflow workflow = test.workflow(BasicWorkflow.class);

      assertEquals("Hello from ADK", workflow.ask("Hello"));
      assertEquals(
          List.of("googleadk.InvokeModel"), AdkTestEnvironment.activities(test.history(workflow)));
      test.replay(test.history(workflow), BasicWorkflowImpl.class);
    }
    // @@@SNIPEND
  }
}
