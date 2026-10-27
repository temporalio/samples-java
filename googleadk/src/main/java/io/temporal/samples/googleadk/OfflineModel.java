package io.temporal.samples.googleadk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OfflineModel extends BaseLlm {
  public OfflineModel() {
    super(AgentSupport.MODEL);
  }

  @Override
  public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
    return Flowable.just(respond(request));
  }

  private static LlmResponse respond(LlmRequest request) {
    if (request.tools().containsKey("getWeather")) {
      Optional<Map<String, Object>> weather = functionResponse(request, "getWeather");
      if (weather.isEmpty()) {
        return call("getWeather", Map.of("city", "Chicago"));
      }
      Optional<Map<String, Object>> converted = functionResponse(request, "toFahrenheit");
      if (converted.isEmpty()) {
        return call("toFahrenheit", Map.of("celsius", weather.get().get("celsius")));
      }
      return text(
          "Chicago is sunny: "
              + weather.get().get("celsius")
              + " C / "
              + converted.get().get("fahrenheit")
              + " F.");
    }
    if (request.tools().containsKey("shipOrder")) {
      return functionResponse(request, "shipOrder")
          .map(
              response ->
                  response.containsKey("error")
                      ? text(
                          response.get("error").toString().contains("rejected")
                              ? "Order rejected."
                              : "Please approve the shipment.")
                      : text("Order shipped."))
          .orElseGet(() -> call("shipOrder", Map.of("orderId", "demo-order")));
    }
    if (request.tools().containsKey("startReport")) {
      return functionResponse(request, "startReport")
          .map(
              response ->
                  text(
                      "done".equals(response.get("status"))
                          ? "Report complete: " + response.get("result")
                          : "Report is pending."))
          .orElseGet(() -> call("startReport", Map.of("name", "sales")));
    }
    if (request.tools().containsKey("echo")) {
      return functionResponse(request, "echo")
          .map(OfflineModel::echo)
          .orElseGet(() -> call("echo", Map.of("text", lastUserText(request))));
    }
    if (request.config().flatMap(config -> config.responseSchema()).isPresent()) {
      return text(
          "{\"city\":\"Chicago\",\"days\":2,\"activities\":[\"Walk the lakefront\",\"Visit a museum\"]}");
    }
    if (request.getSystemInstructions().stream().anyMatch(text -> text.contains("\"planner\""))) {
      return text("Visit Chicago's lakefront.");
    }
    if (request.getSystemInstructions().stream().anyMatch(text -> text.contains("\"reviewer\""))) {
      return text("Bring a warm jacket.");
    }
    long turns =
        request.contents().stream()
            .filter(content -> content.role().orElse("").equals("user"))
            .count();
    return text("turns=" + turns + "; latest: " + lastUserText(request));
  }

  static Optional<Map<String, Object>> functionResponse(LlmRequest request, String name) {
    return request.contents().stream()
        .flatMap(content -> content.parts().orElse(List.of()).stream())
        .flatMap(part -> part.functionResponse().stream())
        .filter(response -> response.name().orElse("").equals(name))
        .map(FunctionResponse::response)
        .flatMap(Optional::stream)
        .reduce((first, last) -> last);
  }

  private static LlmResponse echo(Map<String, Object> response) {
    if (response.get("text_output") instanceof List<?> outputs
        && !outputs.isEmpty()
        && outputs.get(0) instanceof Map<?, ?> first
        && first.get("text") instanceof String value) {
      return text(value);
    }
    throw new IllegalArgumentException("The echo tool returned no text output.");
  }

  private static String lastUserText(LlmRequest request) {
    return request.contents().stream()
        .filter(content -> content.role().orElse("").equals("user"))
        .flatMap(content -> content.parts().orElse(List.of()).stream())
        .flatMap(part -> part.text().stream())
        .reduce((first, last) -> last)
        .orElse("Hello");
  }

  private static LlmResponse text(String value) {
    return LlmResponse.builder()
        .content(Content.builder().role("model").parts(List.of(Part.fromText(value))).build())
        .turnComplete(true)
        .build();
  }

  private static LlmResponse call(String name, Map<String, Object> arguments) {
    return LlmResponse.builder()
        .content(
            Content.builder()
                .role("model")
                .parts(
                    List.of(
                        Part.builder()
                            .functionCall(FunctionCall.builder().name(name).args(arguments).build())
                            .build()))
                .build())
        .build();
  }

  @Override
  public BaseLlmConnection connect(LlmRequest request) {
    throw new UnsupportedOperationException("The offline model has no live connection.");
  }
}
