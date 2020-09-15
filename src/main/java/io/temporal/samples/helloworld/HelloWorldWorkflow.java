package io.temporal.samples.helloworld;
// @@@START java-hello-world-sample-workflow
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloWorldWorkflow implements HelloWorldWorkflowInterface {

  private final HelloWorldActivityInterface activities =
      Workflow.newActivityStub(
          HelloWorldActivityInterface.class,
          ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public String getGreeting(String name) {

    return activities.composeGreeting(name);
  }
}
// @@@END
