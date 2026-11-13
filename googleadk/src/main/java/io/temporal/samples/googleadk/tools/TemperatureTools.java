package io.temporal.samples.googleadk.tools;

import com.google.adk.tools.Annotations.Schema;
import java.util.Map;

public final class TemperatureTools {
  private TemperatureTools() {}

  // @@@SNIPSTART java-google-adk-tools-function
  public static Map<String, Object> toFahrenheit(
      @Schema(name = "celsius", description = "Temperature in degrees Celsius") double celsius) {
    return Map.of("fahrenheit", celsius * 9 / 5 + 32);
  }
  // @@@SNIPEND
}
