package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.structuredoutput.StructuredOutputWorkflow;
import io.temporal.samples.googleadk.structuredoutput.StructuredOutputWorkflowImpl;
import io.temporal.samples.googleadk.structuredoutput.TravelPlan;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(30)
class StructuredOutputWorkflowTest {
  @Test
  void schemaReachesTheModelAndTheWorkflowReturnsATypedPlan() throws Exception {
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request -> {
              var config = request.config().orElseThrow();
              assertEquals("application/json", config.responseMimeType().orElseThrow());
              assertEquals(
                  List.of("city", "days", "activities"),
                  config.responseSchema().orElseThrow().required().orElseThrow());
              return ScriptedModel.text(
                  "{\"city\":\"Chicago\",\"days\":2,\"activities\":[\"Walk the lakefront\"]}");
            });
    try (AdkTestEnvironment test =
        new AdkTestEnvironment(model, StructuredOutputWorkflowImpl.class)) {
      test.environment.start();
      StructuredOutputWorkflow workflow = test.workflow(StructuredOutputWorkflow.class);
      assertEquals(
          new TravelPlan("Chicago", 2, List.of("Walk the lakefront")),
          workflow.plan("Plan two days in Chicago."));
      assertEquals(
          List.of("googleadk.InvokeModel"), AdkTestEnvironment.activities(test.history(workflow)));
      test.replay(test.history(workflow), StructuredOutputWorkflowImpl.class);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not JSON",
        "null",
        "[]",
        "{\"city\":123,\"days\":2,\"activities\":[\"Walk\"]}",
        "{\"city\":\"Chicago\",\"days\":\"2\",\"activities\":[\"Walk\"]}",
        "{\"city\":\"Chicago\",\"days\":8,\"activities\":[\"Walk\"]}",
        "{\"city\":\"Chicago\",\"days\":2,\"activities\":[42]}",
        "{\"city\":\"Chicago\",\"days\":2}",
        "{\"city\":\"Chicago\",\"days\":0,\"activities\":[\"Walk\"]}",
        "{\"city\":\"Chicago\",\"days\":2,\"activities\":[]}",
        "{\"city\":\"Chicago\",\"days\":2.5,\"activities\":[\"Walk\"]}",
        "{\"city\":\"Chicago\",\"days\":null,\"activities\":[\"Walk\"]}",
        "{\"city\":\"Chicago\",\"days\":2,\"activities\":[\"Walk\"]} {}"
      })
  void invalidModelOutputFailsTheWorkflowInsteadOfRetryingItsTask(String response)
      throws Exception {
    ScriptedModel model = ScriptedModel.of(AgentSupport.MODEL, ScriptedModel.text(response));
    try (AdkTestEnvironment test =
        new AdkTestEnvironment(model, StructuredOutputWorkflowImpl.class)) {
      test.environment.start();
      StructuredOutputWorkflow workflow = test.workflow(StructuredOutputWorkflow.class);
      WorkflowFailedException failure =
          assertThrows(
              WorkflowFailedException.class, () -> workflow.plan("Plan two days in Chicago."));
      assertEquals(
          "InvalidTravelPlan",
          assertInstanceOf(ApplicationFailure.class, failure.getCause()).getType());
      assertEquals(1, model.requests().size());
      test.replay(test.history(workflow), StructuredOutputWorkflowImpl.class);
    }
  }
}
