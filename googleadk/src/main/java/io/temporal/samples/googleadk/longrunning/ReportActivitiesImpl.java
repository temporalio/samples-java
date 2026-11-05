package io.temporal.samples.googleadk.longrunning;

import java.util.Map;

public class ReportActivitiesImpl implements ReportActivities {
  @Override
  public Map<String, Object> startReport(String name) {
    return Map.of("name", name, "status", "pending");
  }
}
