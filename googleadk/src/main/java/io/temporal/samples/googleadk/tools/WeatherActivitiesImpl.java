package io.temporal.samples.googleadk.tools;

import java.util.Map;

public class WeatherActivitiesImpl implements WeatherActivities {
  @Override
  public Map<String, Object> getWeather(String city) {
    // Replace this sample response with an HTTP call to your weather provider.
    return Map.of("city", city, "conditions", "sunny", "temperatureF", 72);
  }
}
