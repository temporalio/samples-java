package io.temporal.samples.googleadk.longrunning;

import com.google.adk.tools.Annotations.Schema;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

// @@@SNIPSTART java-google-adk-longrunning-activity
@ActivityInterface
public interface ReportActivities {
  @ActivityMethod(name = "StartReport")
  Map<String, Object> startReport(
      @Schema(name = "name", description = "The report to prepare") String name);
}
// @@@SNIPEND
