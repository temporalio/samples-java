package io.temporal.samples.helloworld;
// @@@START java-hello-world-sample-activity-interface
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface HelloWorldActivityInterface {

  @ActivityMethod
  String composeGreeting(String name);
}
// @@@END