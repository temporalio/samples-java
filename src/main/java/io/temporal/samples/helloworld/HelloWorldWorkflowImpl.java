package io.temporal.samples.helloworld;
// @@@SNIPSTART java-hello-world-sample-workflow
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {
  // Set the ActivityOptions
  ActivityOptions ao =
      ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(2)).build();
  // Create a new ActivityStub for the Activity
  private final HelloWorldActivity activity =
      Workflow.newActivityStub(HelloWorldActivity.class, ao);

  @Override
  public String getGreeting(String name) {
    // Execute the Activity within the Workflow method
    // If there were more Activities, they would be executed from within this method
    return activity.composeGreeting(name);
  }
}
// @@@SNIPEND
