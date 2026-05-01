package io.temporal.samples.springai.chat;

import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link WeatherActivity}.
 *
 * <p>This is a mock implementation that returns simulated weather data. In a real application, this
 * would call an external weather API.
 *
 * <p>Note: This class is registered as a Spring {@code @Component} so it can be auto-discovered.
 * The {@code SpringAiPlugin} will register it with Temporal workers.
 */
@Component
public class WeatherActivityImpl implements WeatherActivity {

  // Mock weather data for demo purposes
  private static final Map<String, String[]> WEATHER_DATA =
      Map.of(
          "seattle", new String[] {"Rainy", "55"},
          "new york", new String[] {"Partly Cloudy", "62"},
          "los angeles", new String[] {"Sunny", "78"},
          "chicago", new String[] {"Windy", "48"},
          "miami", new String[] {"Hot and Humid", "88"},
          "denver", new String[] {"Clear", "45"},
          "boston", new String[] {"Overcast", "52"});

  @Override
  public String getWeather(String city) {
    String normalizedCity = city.toLowerCase(java.util.Locale.ROOT).trim();
    String[] weather = WEATHER_DATA.getOrDefault(normalizedCity, new String[] {"Unknown", "60"});

    int humidity = 40 + new Random().nextInt(40); // 40-80%

    return String.format(
        "Weather in %s: %s, Temperature: %s°F, Humidity: %d%%",
        city, weather[0], weather[1], humidity);
  }

  @Override
  public String getForecast(String city, int days) {
    if (days < 1 || days > 7) {
      return "Error: Days must be between 1 and 7";
    }

    StringBuilder forecast = new StringBuilder();
    forecast.append(String.format("%d-day forecast for %s:\n", days, city));

    String[] conditions = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Clear"};
    Random random = new Random();

    for (int i = 1; i <= days; i++) {
      String condition = conditions[random.nextInt(conditions.length)];
      int high = 50 + random.nextInt(30);
      int low = high - 10 - random.nextInt(10);
      forecast.append(
          String.format("  Day %d: %s, High: %d°F, Low: %d°F\n", i, condition, high, low));
    }

    return forecast.toString();
  }
}
