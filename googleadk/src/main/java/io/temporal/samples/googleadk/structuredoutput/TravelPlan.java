package io.temporal.samples.googleadk.structuredoutput;

import java.util.List;

public record TravelPlan(String city, int days, List<String> activities) {
  public TravelPlan {
    if (city == null || city.isBlank() || days < 1 || days > 7) {
      throw new IllegalArgumentException("A plan needs a city and between one and seven days.");
    }
    if (activities == null
        || activities.isEmpty()
        || activities.stream().anyMatch(activity -> activity == null || activity.isBlank())) {
      throw new IllegalArgumentException("A plan needs at least one nonempty activity.");
    }
    activities = List.copyOf(activities);
  }
}
