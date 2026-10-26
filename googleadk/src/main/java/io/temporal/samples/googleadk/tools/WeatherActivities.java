package io.temporal.samples.googleadk.tools;

import com.google.adk.tools.Annotations.Schema;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

// @@@SNIPSTART java-google-adk-weather-activity
@ActivityInterface
public interface WeatherActivities {
  @ActivityMethod(name = "GetWeather")
  Map<String, Object> getWeather(
      @Schema(name = "city", description = "The city to look up") String city);
}
// @@@SNIPEND
