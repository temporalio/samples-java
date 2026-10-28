package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.temporal.failure.ApplicationFailure;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.observability.ObservabilityWorkflow;
import io.temporal.samples.googleadk.observability.ObservabilityWorkflowImpl;
import io.temporal.samples.googleadk.observability.SampleTelemetry;
import io.temporal.samples.googleadk.tools.WeatherActivities;
import io.temporal.samples.googleadk.tools.WeatherActivitiesImpl;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(30)
class ObservabilityWorkflowTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void modelAndToolSpansShareATraceAndReplayExportsNothing(boolean retryWeather) throws Exception {
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request ->
                AdkTestEnvironment.functionResponse(request, "getWeather")
                    .map(response -> ScriptedModel.text("Chicago is sunny."))
                    .orElseGet(
                        () -> ScriptedModel.functionCall("getWeather", Map.of("city", "Chicago"))));
    try (SampleTelemetry telemetry = new SampleTelemetry(exporter);
        AdkTestEnvironment test =
            new AdkTestEnvironment(
                model, telemetry.workerOptions(), ObservabilityWorkflowImpl.class)) {
      AtomicInteger attempts = new AtomicInteger();
      test.worker.registerActivitiesImplementations(
          (WeatherActivities)
              city -> {
                if (attempts.incrementAndGet() == 1 && retryWeather) {
                  throw ApplicationFailure.newFailure(
                      "Transient weather error", "WeatherUnavailable");
                }
                return new WeatherActivitiesImpl().getWeather(city);
              });
      test.environment.start();
      ObservabilityWorkflow workflow = test.workflow(ObservabilityWorkflow.class);
      assertEquals("Chicago is sunny.", workflow.observe("What's the weather in Chicago?"));
      AdkTestEnvironment.await(
          () ->
              exporter.getFinishedSpanItems().stream()
                  .anyMatch(span -> span.getName().startsWith("RunWorkflow:")));
      var spans = exporter.getFinishedSpanItems();
      assertEquals(
          2,
          spans.stream()
              .filter(span -> span.getName().equals("RunActivity:googleadk.InvokeModel"))
              .count());
      assertEquals(
          retryWeather ? 2 : 1,
          spans.stream().filter(span -> span.getName().equals("RunActivity:GetWeather")).count());
      assertEquals(
          retryWeather,
          spans.stream().anyMatch(span -> span.getStatus().getStatusCode() == StatusCode.ERROR));
      assertTrue(
          spans.stream().allMatch(span -> span.getTraceId().equals(spans.get(0).getTraceId())));
      assertEquals(spans.size(), spans.stream().map(SpanData::getSpanId).distinct().count());
      test.replay(test.history(workflow), ObservabilityWorkflowImpl.class);
      assertEquals(
          spans.size(),
          exporter.getFinishedSpanItems().size(),
          "replay exports no duplicate spans");
    }
  }
}
