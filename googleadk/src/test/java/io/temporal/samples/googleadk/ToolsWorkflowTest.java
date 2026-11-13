package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.tools.ToolsWorkflow;
import io.temporal.samples.googleadk.tools.ToolsWorkflowImpl;
import io.temporal.samples.googleadk.tools.WeatherActivities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class ToolsWorkflowTest {
  @Test
  void weatherToolRetriesAsAnActivityAndItsResultReachesTheModel() throws Exception {
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request ->
                AdkTestEnvironment.functionResponse(request, "toFahrenheit")
                    .map(
                        response -> {
                          assertEquals(68.0, ((Number) response.get("fahrenheit")).doubleValue());
                          return ScriptedModel.text("Chicago is sunny: 20 C / 68 F.");
                        })
                    .orElseGet(
                        () ->
                            AdkTestEnvironment.functionResponse(request, "getWeather")
                                .map(
                                    response -> {
                                      assertEquals("Chicago", response.get("city"));
                                      assertEquals("sunny", response.get("conditions"));
                                      return ScriptedModel.functionCall(
                                          "toFahrenheit",
                                          Map.of("celsius", response.get("celsius")));
                                    })
                                .orElseGet(
                                    () ->
                                        ScriptedModel.functionCall(
                                            "getWeather", Map.of("city", "Chicago")))));
    AtomicInteger attempts = new AtomicInteger();
    WeatherActivities weather =
        city -> {
          if (attempts.incrementAndGet() == 1) {
            throw ApplicationFailure.newFailure("Temporary provider failure", "WeatherUnavailable");
          }
          return Map.of("city", city, "conditions", "sunny", "celsius", 20);
        };
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ToolsWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(weather);
      test.environment.start();
      ToolsWorkflow workflow = test.workflow(ToolsWorkflow.class);

      assertEquals(
          "Chicago is sunny: 20 C / 68 F.", workflow.ask("What's the weather in Chicago?"));
      assertEquals(2, attempts.get());
      assertEquals(3, model.requests().size());
      assertEquals(
          List.of(
              "googleadk.InvokeModel",
              "GetWeather",
              "googleadk.InvokeModel",
              "googleadk.InvokeModel"),
          AdkTestEnvironment.activities(test.history(workflow)));
      test.replay(test.history(workflow), ToolsWorkflowImpl.class);
      assertEquals(2, attempts.get(), "replay must not repeat the weather call");
    }
  }

  @Test
  void aNonRetryableToolFailureFailsTheWorkflowWithoutAnotherModelTurn() throws Exception {
    ScriptedModel model =
        ScriptedModel.of(
            AgentSupport.MODEL,
            ScriptedModel.functionCall("getWeather", Map.of("city", "Chicago")));
    AtomicInteger attempts = new AtomicInteger();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ToolsWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(
          (WeatherActivities)
              city -> {
                attempts.incrementAndGet();
                throw ApplicationFailure.newNonRetryableFailure(
                    "Weather access denied", "WeatherDenied");
              });
      test.environment.start();
      ToolsWorkflow workflow = test.workflow(ToolsWorkflow.class);
      WorkflowFailedException failure =
          assertThrows(
              WorkflowFailedException.class, () -> workflow.ask("What's the weather in Chicago?"));
      ActivityFailure activityFailure = assertInstanceOf(ActivityFailure.class, failure.getCause());
      assertEquals(
          "WeatherDenied",
          assertInstanceOf(ApplicationFailure.class, activityFailure.getCause()).getType());
      assertEquals(1, attempts.get());
      assertEquals(1, model.requests().size());
      test.replay(test.history(workflow), ToolsWorkflowImpl.class);
    }
  }
}
