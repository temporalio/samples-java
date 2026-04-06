package io.temporal.samples.springai.chat;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Activity interface for weather-related operations.
 *
 * <p>This demonstrates how to combine Temporal's {@link ActivityInterface} with Spring AI's {@link
 * Tool} annotation to create activity-based AI tools.
 *
 * <p>When passed to {@code TemporalChatClient.builder().defaultTools(weatherActivity)}, the AI
 * model can call these methods, and they will execute as durable Temporal activities with automatic
 * retries and timeout handling.
 */
@ActivityInterface
public interface WeatherActivity {

  /**
   * Gets the current weather for a city.
   *
   * <p>The {@code @Tool} annotation makes this method available to the AI model, while the
   * {@code @ActivityInterface} ensures it executes as a Temporal activity.
   *
   * @param city the name of the city
   * @return a description of the current weather
   */
  @Tool(
      description =
          "Get the current weather for a city. Returns temperature, conditions, and humidity.")
  @ActivityMethod
  String getWeather(
      @ToolParam(description = "The name of the city (e.g., 'Seattle', 'New York')") String city);

  /**
   * Gets the weather forecast for a city.
   *
   * @param city the name of the city
   * @param days the number of days to forecast (1-7)
   * @return the weather forecast
   */
  @Tool(description = "Get the weather forecast for a city for the specified number of days.")
  @ActivityMethod
  String getForecast(
      @ToolParam(description = "The name of the city") String city,
      @ToolParam(description = "Number of days to forecast (1-7)") int days);
}
