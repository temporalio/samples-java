package io.temporal.samples.googleadk.tools;

import java.time.Instant;
import java.util.Map;

public class WeatherActivitiesImpl implements WeatherActivities {
  @Override
  public Map<String, Object> getWeather(String city) {
    return Map.of(
        "city", city, "conditions", "sunny", "celsius", 20, "observedAt", Instant.now().toString());
  }
}
