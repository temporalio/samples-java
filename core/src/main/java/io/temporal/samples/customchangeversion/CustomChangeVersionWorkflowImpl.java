package io.temporal.samples.customchangeversion;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * CustomChangeVersionWorkflowImpl shows how to upsert a custom search attribute which can be used
 * when adding changes to our workflow using workflow versioning. Note that this is only temporary
 * solution until https://github.com/temporalio/sdk-java/issues/587 is implemented. Given that a
 * number of users are in need of this and are looking for a sample, we are adding this as sample
 * until this issue is fixed, at which point it will no longer be needed.
 */
public class CustomChangeVersionWorkflowImpl implements CustomChangeVersionWorkflow {
  static final SearchAttributeKey<String> CUSTOM_CHANGE_VERSION =
      SearchAttributeKey.forKeyword("CustomChangeVersion");
  private CustomChangeVersionActivities activities =
      Workflow.newActivityStub(
          CustomChangeVersionActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public String run(String input) {
    String result = activities.customOne(input);
    // We assume when this change is added we have some executions of this workflow type running
    // where customTwo activity was not called (we are adding it to exiting workflow)
    // Adding customTwo activity as a versioned change
    int version = Workflow.getVersion("add-v2-activity-change", Workflow.DEFAULT_VERSION, 1);
    if (version == 1) {
      // Upsert our custom change version search attribute
      // We set its value to follow TemporalChangeVersion structure of "<changeId>-<version>"
      Workflow.upsertTypedSearchAttributes(
          CUSTOM_CHANGE_VERSION.valueUnset(),
          CUSTOM_CHANGE_VERSION.valueSet("add-v2-activity-change-1"));
      // Adding call to v2 activity
      result += activities.customTwo(input);
    }

    // lets say then later on we also want to add a change to invoke another activity
    version = Workflow.getVersion("add-v3-activity-change", Workflow.DEFAULT_VERSION, 1);
    if (version == 1) {
      // Upsert our custom change version search attribute
      // We set its value to follow TemporalChangeVersion structure of "<changeId>-<version>"
      Workflow.upsertTypedSearchAttributes(
          CUSTOM_CHANGE_VERSION.valueUnset(),
          CUSTOM_CHANGE_VERSION.valueSet("add-v3-activity-change-1"));
      // Adding call to v2 activity
      result += activities.customThree(input);
    }
    return result;
  }
}
