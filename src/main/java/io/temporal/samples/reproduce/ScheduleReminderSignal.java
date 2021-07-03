package io.temporal.samples.reproduce;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

public final class ScheduleReminderSignal {

  private final Instant reminderTime;
  private final String reminderText;

  @JsonCreator
  public ScheduleReminderSignal(
      @JsonProperty("reminderTime") Instant reminderTime,
      @JsonProperty("reminderText") String reminderText) {
    this.reminderTime = reminderTime;
    this.reminderText = reminderText;
  }

  @JsonProperty("reminderTime")
  public Instant getReminderTime() {
    return reminderTime;
  }

  @JsonProperty("reminderText")
  public String getReminderText() {
    return reminderText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScheduleReminderSignal that = (ScheduleReminderSignal) o;
    return Objects.equals(reminderTime, that.reminderTime)
        && Objects.equals(reminderText, that.reminderText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reminderTime, reminderText);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ScheduleReminderSignal.class.getSimpleName() + "[", "]")
        .add("reminderTime=" + reminderTime)
        .add("reminderText='" + reminderText + "'")
        .toString();
  }
}
