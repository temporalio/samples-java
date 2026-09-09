package io.temporal.samples.gcp.cloudrun;

import static org.junit.Assert.assertEquals;

import io.temporal.gcp.cloudrun.CloudRunOpenTelemetryPlugin;
import java.time.Duration;
import org.junit.Test;

public class CloudRunPluginDefaultsTest {
  @Test
  public void usesCoordinatedMetricExportInterval() {
    assertEquals(
        Duration.ofSeconds(60), CloudRunOpenTelemetryPlugin.DEFAULT_METRICS_REPORT_INTERVAL);
  }
}
