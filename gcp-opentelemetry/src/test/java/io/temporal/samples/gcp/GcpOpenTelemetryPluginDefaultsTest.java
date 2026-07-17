package io.temporal.samples.gcp;

import static org.junit.Assert.assertEquals;

import io.temporal.gcp.GcpOpenTelemetryPlugin;
import java.time.Duration;
import org.junit.Test;

public class GcpOpenTelemetryPluginDefaultsTest {
  @Test
  public void usesCoordinatedMetricExportInterval() {
    assertEquals(Duration.ofSeconds(60), GcpOpenTelemetryPlugin.DEFAULT_METRICS_REPORT_INTERVAL);
  }
}
